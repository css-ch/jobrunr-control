package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.monitoring.GetBatchProgressUseCase;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionHistoryUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobStatus;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UI Controller for job execution history.
 * Renders execution details and batch progress using type-safe Qute templates.
 */
@Path("/q/jobrunr-control/history")
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
                                                                    String sortBy, String sortOrder);

        public static native TemplateInstance batchProgress(BatchProgress progress);
    }

    private final GetJobExecutionHistoryUseCase getHistoryUseCase;
    private final ResolveParametersUseCase resolveParametersUseCase;
    private final GetBatchProgressUseCase getBatchProgressUseCase;

    @Inject
    public JobExecutionsController(
            GetJobExecutionHistoryUseCase getHistoryUseCase,
            ResolveParametersUseCase resolveParametersUseCase,
            GetBatchProgressUseCase getBatchProgressUseCase) {
        this.getHistoryUseCase = getHistoryUseCase;
        this.resolveParametersUseCase = resolveParametersUseCase;
        this.getBatchProgressUseCase = getBatchProgressUseCase;
    }

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getExecutionHistoryView() {
        return Templates.executionHistory();
    }

    @GET
    @Path("/table")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getExecutionHistoryTable(
            @QueryParam("search") String search,
            @QueryParam("status-filter") @DefaultValue("all") String statusFilter,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("startedAt") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("desc") String sortOrder) {

        LOG.infof("getExecutionHistoryTable called with page=%d, size=%d, sortBy=%s, sortOrder=%s, search=%s, statusFilter=%s",
                page, size, sortBy, sortOrder, search, statusFilter);

        List<JobExecutionInfo> executions = getHistoryUseCase.execute();

        // Filter nach Status
        if (statusFilter != null && !"all".equals(statusFilter)) {
            final JobStatus filterStatus = JobStatus.valueOf(statusFilter);
            executions = executions.stream()
                    .filter(e -> e.getStatus() == filterStatus)
                    .toList();
        }

        executions = JobSearchUtils.applySearchToExecutions(search, executions);

        // Sortierung anwenden
        Comparator<JobExecutionInfo> comparator = getExecutionComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        executions = executions.stream()
                .sorted(comparator)
                .toList();

        // Pagination anwenden
        PaginationHelper.PaginationResult<JobExecutionInfo> paginationResult = PaginationHelper.paginate(executions, page, size);

        LOG.infof("Returning %d executions to template (expected max: %d)", paginationResult.pageItems().size(), size);

        return Components.executionHistoryTable(
                paginationResult.pageItems(),
                paginationResult.metadata(),
                paginationResult.pageRange(),
                search != null ? search : "",
                statusFilter,
                sortBy,
                sortOrder
        );
    }

    @GET
    @Path("/{id}/batch-progress")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public String getBatchProgressFragment(@PathParam("id") UUID jobId) {
        Optional<BatchProgress> progress = getBatchProgressUseCase.execute(jobId);

        if (progress.isPresent()) {
            return Components.batchProgress(progress.get()).render();
        } else {
            return "<small>Kein Batch-Job</small>";
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
