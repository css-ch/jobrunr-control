package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.monitoring.GetBatchProgressUseCase;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionHistoryUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobStatus;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UI Controller for job execution history.
 * Renders execution details and batch progress using type-safe Qute templates.
 */
@ApplicationScoped
public class JobExecutionsController {

    private static final Logger LOG = Logger.getLogger(JobExecutionsController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {

        private Templates() {
            // Utility class
        }

        public static native TemplateInstance executionHistory();
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {

        private Components() {
            // Utility class
        }

        public static native TemplateInstance executionHistoryTable(List<JobExecutionInfo> executions,
                                                                    PaginationHelper.PaginationMetadata pagination,
                                                                    List<TemplateExtensions.PageItem> pageRange,
                                                                    String search, String statusFilter,
                                                                    String sortBy, String sortOrder,
                                                                    boolean showUuid, String host, String port);

        public static native TemplateInstance batchProgress(BatchProgress progress);
    }

    private final GetJobExecutionHistoryUseCase getHistoryUseCase;
    private final ResolveParametersUseCase resolveParametersUseCase;
    private final GetBatchProgressUseCase getBatchProgressUseCase;
    private final JobRunrControlUiConfig uiConfig;

    @Inject
    public JobExecutionsController(
            GetJobExecutionHistoryUseCase getHistoryUseCase,
            ResolveParametersUseCase resolveParametersUseCase,
            GetBatchProgressUseCase getBatchProgressUseCase,
            JobRunrControlUiConfig uiConfig) {
        this.getHistoryUseCase = getHistoryUseCase;
        this.resolveParametersUseCase = resolveParametersUseCase;
        this.getBatchProgressUseCase = getBatchProgressUseCase;
        this.uiConfig = uiConfig;
    }

    public void handleIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, Templates.executionHistory());
    }

    public void handleTable(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }

        String search = UiRoutingSupport.queryParam(ctx, "search");
        String statusFilter = UiRoutingSupport.queryParam(ctx, "status-filter", "all");
        int page = UiRoutingSupport.intQueryParam(ctx, "page", 0);
        int size = UiRoutingSupport.intQueryParam(ctx, "size", 10);
        String sortBy = UiRoutingSupport.queryParam(ctx, "sortBy", "startedAt");
        String sortOrder = UiRoutingSupport.queryParam(ctx, "sortOrder", "desc");

        LOG.infof("handleTable page=%d, size=%d, sortBy=%s, sortOrder=%s, search=%s, statusFilter=%s",
                page, size, sortBy, sortOrder, search, statusFilter);

        String host = ctx.request().authority() != null ? ctx.request().authority().host() : "";
        String port = ctx.request().authority() != null ? String.valueOf(ctx.request().authority().port()) : "";

        List<JobExecutionInfo> executions = getHistoryUseCase.execute();

        if (statusFilter != null && !"all".equals(statusFilter)) {
            final JobStatus filterStatus = JobStatus.valueOf(statusFilter);
            executions = executions.stream()
                    .filter(e -> e.getStatus() == filterStatus)
                    .toList();
        }

        executions = JobSearchUtils.applySearchToExecutions(search, executions);

        Comparator<JobExecutionInfo> comparator = getExecutionComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        executions = executions.stream()
                .sorted(comparator)
                .toList();

        PaginationHelper.PaginationResult<JobExecutionInfo> paginationResult = PaginationHelper.paginate(executions, page, size);

        LOG.infof("Returning %d executions to template (expected max: %d)", paginationResult.pageItems().size(), size);

        UiRoutingSupport.renderHtml(ctx, Components.executionHistoryTable(
                paginationResult.pageItems(),
                paginationResult.metadata(),
                paginationResult.pageRange(),
                search != null ? search : "",
                statusFilter,
                sortBy,
                sortOrder,
                uiConfig.showJobUuid(),
                host,
                port
        ));
    }

    public void handleBatchProgress(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        Optional<BatchProgress> progress = getBatchProgressUseCase.execute(jobId);

        if (progress.isPresent()) {
            UiRoutingSupport.renderHtml(ctx, Components.batchProgress(progress.get()).render());
        } else {
            UiRoutingSupport.renderHtml(ctx, "<small>Kein Batch-Job</small>");
        }
    }

    private Comparator<JobExecutionInfo> getExecutionComparator(String sortBy) {
        return switch (sortBy) {
            case "jobName" -> Comparator.comparing(JobExecutionInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
            case "jobType" -> Comparator.comparing(JobExecutionInfo::getJobType, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(e -> e.getStatus().name());
            case "finishedAt" -> Comparator.comparing(e -> e.getFinishedAt().orElse(null),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(JobExecutionInfo::getStartedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }
}
