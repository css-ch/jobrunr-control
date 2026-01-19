package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.monitoring.GetBatchProgressUseCase;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionHistoryUseCase;
import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobStatus;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;

/**
 * UI Controller for job execution history.
 * Renders execution details and batch progress.
 */
@Path("/history")
public class JobExecutionsController {

    @Inject
    @io.quarkus.qute.Location("execution-history.html")
    Template executionHistory;

    @Inject
    @io.quarkus.qute.Location("components/execution-history-table.html")
    Template executionHistoryTable;

    @Inject
    @io.quarkus.qute.Location("components/batch-progress.html")
    Template batchProgress;

    @Inject
    GetJobExecutionHistoryUseCase getHistoryUseCase;

    @Inject
    GetBatchProgressUseCase getBatchProgressUseCase;

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getExecutionHistoryView() {
        return executionHistory.instance();
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
        long totalElements = executions.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        page = Math.max(0, Math.min(page, totalPages - 1)); // Ensure page is in valid range

        int start = Math.min(page * size, executions.size());
        int end = Math.min(start + size, executions.size());
        List<JobExecutionInfo> pageItems = executions.subList(start, end);

        // Pagination metadata
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages - 1);
        pagination.put("hasPrevious", page > 0);
        pagination.put("nextPage", page < totalPages - 1 ? page + 1 : page);
        pagination.put("previousPage", page > 0 ? page - 1 : 0);
        pagination.put("lastPage", Math.max(0, totalPages - 1));
        pagination.put("isEmpty", pageItems.isEmpty());

        // Compute display values for pagination info
        int startItem = totalElements > 0 ? (page * size + 1) : 0;
        int endItem = totalElements > 0 ? Math.min((page + 1) * size, (int) totalElements) : 0;
        pagination.put("startItem", startItem);
        pagination.put("endItem", endItem);

        // Compute page range for pagination controls
        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        return executionHistoryTable
                .data("executions", pageItems)
                .data("pagination", pagination)
                .data("pageRange", pageRange)
                .data("search", search != null ? search : "")
                .data("statusFilter", statusFilter)
                .data("sortBy", sortBy)
                .data("sortOrder", sortOrder);
    }

    private Comparator<JobExecutionInfo> getExecutionComparator(String sortBy) {
        return switch (sortBy) {
            case "jobName" -> Comparator.comparing(JobExecutionInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
            case "jobType" -> Comparator.comparing(JobExecutionInfo::getJobType, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(e -> e.getStatus().name());
            case "startedAt" -> Comparator.comparing(JobExecutionInfo::getStartedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "finishedAt" -> Comparator.comparing(e -> e.getFinishedAt().orElse(null),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(JobExecutionInfo::getStartedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    @GET
    @Path("/{id}/batch-progress")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public String getBatchProgressFragment(@PathParam("id") UUID jobId) {
        Optional<BatchProgress> progress = getBatchProgressUseCase.execute(jobId);

        if (progress.isPresent()) {
            return batchProgress.data("progress", progress.get()).render();
        } else {
            return "<small>Kein Batch-Job</small>";
        }
    }
}
