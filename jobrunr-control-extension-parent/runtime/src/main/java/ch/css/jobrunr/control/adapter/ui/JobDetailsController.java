package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.details.GetJobDetailsMessagesAsCsvUseCase;
import ch.css.jobrunr.control.application.details.GetJobDetailsMessageUseCase;
import ch.css.jobrunr.control.application.details.GetJobDetailsParametersUseCase;
import ch.css.jobrunr.control.application.details.GetJobDetailsRecapUseCase;
import ch.css.jobrunr.control.application.details.GetExpectedJobDeletionTimeUseCase;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.details.JobMessage;
import ch.css.jobrunr.control.domain.details.JobMessageAttemptFilter;
import ch.css.jobrunr.control.domain.details.JobMessageLevelSearch;
import ch.css.jobrunr.control.domain.details.JobMessageSortOrder;
import ch.css.jobrunr.control.domain.details.JobMessagesPaged;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Main Dashboard Controller.
 * Root handler delegates to the scheduled-jobs template.
 */
@ApplicationScoped
public class JobDetailsController {

    private final GetJobDetailsParametersUseCase getJobDetailsParametersUseCase;
    private final GetJobDetailsRecapUseCase getJobDetailsRecapUseCase;
    private final GetJobDetailsMessageUseCase getJobDetailsMessageUseCase;
    private final GetJobDetailsMessagesAsCsvUseCase getJobDetailsMessagesAsCsvUseCase;
    private final JobRunrControlUiConfig uiConfig;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final GetExpectedJobDeletionTimeUseCase getExpectedJobDeletionTimeUseCase;
    private static final DateTimeFormatter DELETION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    @Inject
    public JobDetailsController(GetJobDetailsParametersUseCase getJobDetailsParametersUseCase,
                                GetJobDetailsRecapUseCase getJobDetailsRecapUseCase,
                                GetJobDetailsMessageUseCase getJobDetailsMessageUseCase,
                                GetJobDetailsMessagesAsCsvUseCase getJobDetailsMessagesAsCsvUseCase,
                                JobRunrControlUiConfig uiConfig,
                                JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                GetExpectedJobDeletionTimeUseCase getExpectedJobDeletionTimeUseCase) {
        this.getJobDetailsParametersUseCase = getJobDetailsParametersUseCase;
        this.getJobDetailsRecapUseCase = getJobDetailsRecapUseCase;
        this.getJobDetailsMessageUseCase = getJobDetailsMessageUseCase;
        this.getJobDetailsMessagesAsCsvUseCase = getJobDetailsMessagesAsCsvUseCase;
        this.uiConfig = uiConfig;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.getExpectedJobDeletionTimeUseCase = getExpectedJobDeletionTimeUseCase;
    }

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        private Templates() {
            // Utility class
        }

        public static native TemplateInstance jobDetails(String jobId, String jobType, String jobName,
                                                         boolean showExtendedDetails,
                                                         String expectedDeletionTime);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        private Components() {
            // Utility class
        }

        public static native TemplateInstance jobDetailsRecap(GetJobDetailsRecapUseCase.Result recap,
                                                              boolean showBusinessStatus,
                                                              boolean showExtendedDetails);

        public static native TemplateInstance jobDetailsParameter(GetJobDetailsParametersUseCase.Result parameter);

        public static native TemplateInstance jobDetailsMessages(MessagesPaginationResult messages);
    }

    public void handleIndex(RoutingContext ctx) {
        PerformanceLogger plog = new PerformanceLogger("handleIndex");
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobId = UiRoutingSupport.queryParam(ctx, "jobId");
        String jobType = UiRoutingSupport.queryParam(ctx, "jobType");
        String jobName = UiRoutingSupport.queryParam(ctx, "jobName");
        String expectedDeletionTime = getExpectedJobDeletionTimeUseCase.execute(jobIdAsUUID(jobId))
                .map(DELETION_TIME_FORMATTER::format)
                .orElse(null);

        // Construct title and subtitle on Java side (no template interpolation needed)
        UiRoutingSupport.renderHtml(ctx, JobDetailsController.Templates.jobDetails(
                jobId, jobType, jobName, hasDetailPage(jobType), expectedDeletionTime));
        plog.log();
    }

    public void handleDetailsRecap(RoutingContext ctx) {
        PerformanceLogger plog = new PerformanceLogger("handleDetailsRecap");
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildRecapTable(
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType")
        ));
        plog.log();
    }

    public void handleDetailsParameter(RoutingContext ctx) {
        PerformanceLogger plog = new PerformanceLogger("handleDetailsParameter");
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildParameterTable(
                UiRoutingSupport.queryParam(ctx, "jobId")
        ));
        plog.log();
    }

    public void handleDetailsParameterDownload(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobId = UiRoutingSupport.queryParam(ctx, "jobId");
        GetJobDetailsParametersUseCase.Result result = getJobDetailsParametersUseCase.execute(jobId);

        String fileName = "batch-parameters-" + jobId + ".json";
        ctx.response()
                .putHeader("Content-Type", "application/json; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .end(Json.encodePrettily(result.parameters()));
    }

    public void handleDetailsMessages(RoutingContext ctx) {
        PerformanceLogger plog = new PerformanceLogger("handleDetailsMessages");
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        int page = UiRoutingSupport.intQueryParam(ctx, "page", 0);
        int size = UiRoutingSupport.intQueryParam(ctx, "size", 10);
        UiRoutingSupport.renderHtml(ctx, buildMessagesTable(
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType"),
                UiRoutingSupport.queryParam(ctx, "search"),
                UiRoutingSupport.queryParam(ctx, "textSearch"),
                UiRoutingSupport.queryParam(ctx, "sortOrder"),
                UiRoutingSupport.queryParam(ctx, "attemptFilter"),
                page,
                size));
        plog.log();
    }

    public void handleDetailsMessagesDownload(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobId = UiRoutingSupport.queryParam(ctx, "jobId");
        String jobType = UiRoutingSupport.queryParam(ctx, "jobType");
        JobMessageLevelSearch levelSearch = searchMessageLevel(UiRoutingSupport.queryParam(ctx, "search"));
        String textSearch = UiRoutingSupport.queryParam(ctx, "textSearch");
        JobMessageSortOrder sortOrder = parseSortOrder(UiRoutingSupport.queryParam(ctx, "sortOrder"));
        JobMessageAttemptFilter attemptFilter = parseAttemptFilter(UiRoutingSupport.queryParam(ctx, "attemptFilter"));

        String csvContent = getJobDetailsMessagesAsCsvUseCase.execute(
                jobIdAsUUID(jobId), jobType, levelSearch, textSearch, sortOrder, attemptFilter);

        String fileName = "messages-" + jobId + ".csv";
        ctx.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .end(csvContent);
    }

    private TemplateInstance buildRecapTable(String jobId, String jobType) {
        GetJobDetailsRecapUseCase.Result recapData = getJobDetailsRecapUseCase.execute(jobIdAsUUID(jobId));
        return JobDetailsController.Components.jobDetailsRecap(
                recapData,
                uiConfig.showBusinessStatus(),
                hasDetailPage(jobType)
        );
    }

    private boolean hasDetailPage(String jobType) {
        Optional<JobDefinition> jobDefinition = jobDefinitionDiscoveryService.findJobByType(jobType);
        return jobDefinition.isPresent() && jobDefinition.get().jobDetailPage() != null;
    }

    private TemplateInstance buildParameterTable(String jobId) {
        try {
            GetJobDetailsParametersUseCase.Result result = getJobDetailsParametersUseCase.execute(jobId);
            return JobDetailsController.Components.jobDetailsParameter(result);
            //lsParameter(result.parameters(), result.parameterSections(), result.parameterDefinitions(), result.showEmptyParameters());
        } catch (Exception e) {
            // Return with empty parameters if loading fails
            return JobDetailsController.Components.jobDetailsParameter(new GetJobDetailsParametersUseCase.Result(Map.of(), List.of(), List.of(), false));
        }
    }

    private TemplateInstance buildMessagesTable(String jobId,
                                                String jobType,
                                                String search,
                                                String textSearch,
                                                String sortOrder,
                                                String attemptFilter,
                                                int page,
                                                int size) {
        JobMessagesPaged result = getJobDetailsMessageUseCase.execute(
                jobIdAsUUID(jobId),
                jobType,
                searchMessageLevel(search),
                textSearch,
                parseSortOrder(sortOrder),
                parseAttemptFilter(attemptFilter),
                page,
                size
        );
        return JobDetailsController.Components.jobDetailsMessages(toMessagesPaginationResult(result));
    }

    private UUID jobIdAsUUID(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Job ID must not be null or empty");
        }
        try {
            return UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        }
    }

    private JobMessageLevelSearch searchMessageLevel(String search) {
        if (search == null || search.isBlank()) {
            return JobMessageLevelSearch.ALL;
        }
        try {
            return JobMessageLevelSearch.valueOf(search);
        } catch (IllegalArgumentException e) {
            return JobMessageLevelSearch.ALL;
        }
    }

    private JobMessageSortOrder parseSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return JobMessageSortOrder.OLDEST_FIRST;
        }
        try {
            return JobMessageSortOrder.valueOf(sortOrder);
        } catch (IllegalArgumentException e) {
            return JobMessageSortOrder.OLDEST_FIRST;
        }
    }

    private JobMessageAttemptFilter parseAttemptFilter(String attemptFilter) {
        if (attemptFilter == null || attemptFilter.isBlank()) {
            return JobMessageAttemptFilter.LATEST_ONLY;
        }
        try {
            return JobMessageAttemptFilter.valueOf(attemptFilter);
        } catch (IllegalArgumentException e) {
            return JobMessageAttemptFilter.LATEST_ONLY;
        }
    }

    private MessagesPaginationResult toMessagesPaginationResult(JobMessagesPaged jobMessagesPaged) {
        PaginationHelper.PaginationMetadata paginationMetadata = PaginationHelper.createPaginationMetadata(
                jobMessagesPaged.page(),
                jobMessagesPaged.pageSize(),
                jobMessagesPaged.totalMessages()
        );
        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(paginationMetadata);
        return new MessagesPaginationResult(jobMessagesPaged.messages(), paginationMetadata, pageRange);
    }

    public record MessagesPaginationResult(
            List<JobMessage> pageItems,
            PaginationHelper.PaginationMetadata pagination,
            List<TemplateExtensions.PageItem> pageRange
    ) {
    }
}
