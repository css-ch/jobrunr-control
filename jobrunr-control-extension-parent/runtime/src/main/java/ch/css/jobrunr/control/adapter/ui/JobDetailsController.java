package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.details.GetJobDetailsMessageUseCase;
import ch.css.jobrunr.control.application.details.GetJobDetailsParametersUseCase;
import ch.css.jobrunr.control.application.details.GetJobDetailsRecapUseCase;
import ch.css.jobrunr.control.domain.details.JobMessage;
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
import java.util.UUID;

/**
 * Main Dashboard Controller.
 * Root handler delegates to the scheduled-jobs template.
 */
@ApplicationScoped
public class JobDetailsController {

    private final GetJobDetailsParametersUseCase getJobDetailsParametersUseCase;
    private final GetJobDetailsRecapUseCase getJobDetailsRecapUseCase;
    private final GetJobDetailsMessageUseCase getJobDetailsMessageUseCase;

    @Inject
    public JobDetailsController(GetJobDetailsParametersUseCase getJobDetailsParametersUseCase, GetJobDetailsRecapUseCase getJobDetailsRecapUseCase, GetJobDetailsMessageUseCase getJobDetailsMessageUseCase) {
        this.getJobDetailsParametersUseCase = getJobDetailsParametersUseCase;
        this.getJobDetailsRecapUseCase = getJobDetailsRecapUseCase;
        this.getJobDetailsMessageUseCase = getJobDetailsMessageUseCase;
    }

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        private Templates() {
            // Utility class
        }

        public static native TemplateInstance jobDetails(String jobId, String jobType, String jobName);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        private Components() {
            // Utility class
        }

        public static native TemplateInstance jobDetailsRecap(GetJobDetailsRecapUseCase.Result recap);
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

        // Construct title and subtitle on Java side (no template interpolation needed)
        UiRoutingSupport.renderHtml(ctx, JobDetailsController.Templates.jobDetails(jobId, jobType, jobName));
        plog.log();
    }

    public void handleDetailsRecap(RoutingContext ctx) {
        PerformanceLogger plog = new PerformanceLogger("handleDetailsRecap");
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildRecapTable(
                UiRoutingSupport.queryParam(ctx, "jobId")
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
                page,
                size));
        plog.log();
    }

    private TemplateInstance buildRecapTable(String jobId) {
        GetJobDetailsRecapUseCase.Result recapData = getJobDetailsRecapUseCase.execute(jobIdAsUUID(jobId));
        return JobDetailsController.Components.jobDetailsRecap(recapData);
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
                                                int page,
                                                int size) {
        JobMessagesPaged result = getJobDetailsMessageUseCase.execute(
                jobIdAsUUID(jobId),
                jobType,
                searchMessageLevel(search),
                textSearch,
                parseSortOrder(sortOrder),
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

