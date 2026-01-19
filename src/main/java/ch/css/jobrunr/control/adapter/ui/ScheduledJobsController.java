package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.application.scheduling.*;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI Controller for scheduled jobs.
 * Renders Qute templates and processes HTMX requests.
 */
@Path("/scheduled")
public class ScheduledJobsController {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobsController.class);

    @Inject
    @io.quarkus.qute.Location("scheduled-jobs.html")
    Template scheduledJobs;

    @Inject
    @io.quarkus.qute.Location("components/scheduled-jobs-table.html")
    Template scheduledJobsTable;

    @Inject
    @io.quarkus.qute.Location("modals/job-form.html")
    Template jobForm;

    @Inject
    @io.quarkus.qute.Location("components/param-inputs.html")
    Template paramInputs;

    @Inject
    DiscoverJobsUseCase discoverJobsUseCase;

    @Inject
    GetJobParametersUseCase getJobParametersUseCase;

    @Inject
    GetScheduledJobsUseCase getScheduledJobsUseCase;

    @Inject
    GetScheduledJobByIdUseCase getScheduledJobByIdUseCase;

    @Inject
    CreateScheduledJobUseCase createJobUseCase;

    @Inject
    UpdateScheduledJobUseCase updateJobUseCase;

    @Inject
    DeleteScheduledJobUseCase deleteJobUseCase;

    @Inject
    ExecuteJobImmediatelyUseCase executeJobUseCase;

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getScheduledJobsView() {
        return scheduledJobs.instance();
    }

    @GET
    @Path("/table")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getScheduledJobsTable(
            @QueryParam("search") String search,
            @QueryParam("filter") @DefaultValue("all") String filter,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("scheduledAt") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {

        List<ScheduledJobInfo> jobs = getScheduledJobsUseCase.execute();

        // Filter anwenden
        if ("external".equals(filter)) {
            jobs = jobs.stream()
                    .filter(ScheduledJobInfo::isExternallyTriggerable)
                    .toList();
        } else if ("scheduled".equals(filter)) {
            jobs = jobs.stream()
                    .filter(j -> !j.isExternallyTriggerable())
                    .toList();
        }

        // Suche anwenden
        jobs = JobSearchUtils.applySearchToScheduledJobs(search, jobs);

        // Sortierung anwenden
        Comparator<ScheduledJobInfo> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        jobs = jobs.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        // Pagination anwenden
        long totalElements = jobs.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        page = Math.max(0, Math.min(page, totalPages - 1)); // Ensure page is in valid range

        int start = Math.min(page * size, jobs.size());
        int end = Math.min(start + size, jobs.size());
        List<ScheduledJobInfo> pageItems = jobs.subList(start, end);

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

        // Compute display values for pagination info (Qute doesn't support complex arithmetic)
        int startItem = totalElements > 0 ? (page * size + 1) : 0;
        int endItem = totalElements > 0 ? Math.min((page + 1) * size, (int) totalElements) : 0;
        pagination.put("startItem", startItem);
        pagination.put("endItem", endItem);

        // Compute page range for pagination controls
        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        return scheduledJobsTable
                .data("jobs", pageItems)
                .data("pagination", pagination)
                .data("pageRange", pageRange)
                .data("search", search != null ? search : "")
                .data("filter", filter)
                .data("sortBy", sortBy)
                .data("sortOrder", sortOrder);
    }

    private Comparator<ScheduledJobInfo> getComparator(String sortBy) {
        return switch (sortBy) {
            case "jobName" -> Comparator.comparing(ScheduledJobInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
            case "jobType" -> Comparator.comparing(ScheduledJobInfo::getJobType, String.CASE_INSENSITIVE_ORDER);
            case "scheduledAt" -> Comparator.comparing(ScheduledJobInfo::getScheduledAt);
            default -> Comparator.comparing(ScheduledJobInfo::getScheduledAt);
        };
    }

    @GET
    @Path("/modal/new")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getNewJobModal() {
        List<JobDefinition> jobDefinitions = discoverJobsUseCase.execute();

        return jobForm
                .data("jobDefinitions", jobDefinitions)
                .data("isEdit", false);
    }

    @GET
    @Path("/modal/{id}/edit")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getEditJobModal(@PathParam("id") UUID jobId) {
        List<JobDefinition> jobDefinitions = discoverJobsUseCase.execute();

        ScheduledJobInfo jobInfo = getScheduledJobByIdUseCase.execute(jobId)
                .orElseThrow(() -> new NotFoundException("Job nicht gefunden: " + jobId));

        // Load parameter definitions for this job type
        Set<JobParameter> parameters = Set.of();
        try {
            parameters = getJobParametersUseCase.execute(jobInfo.getJobType());
            log.info("Loaded {} parameter definitions for job type '{}' in edit mode", parameters.size(), jobInfo.getJobType());
        } catch (Exception e) {
            log.error("Error loading parameters for job type '{}': {}", jobInfo.getJobType(), e.getMessage(), e);
        }

        return jobForm
                .data("jobDefinitions", jobDefinitions)
                .data("isEdit", true)
                .data("job", jobInfo)
                .data("parameters", parameters);
    }

    @GET
    @Path("/modal/parameters")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getJobParameters(@QueryParam("jobType") String jobType) {
        log.info("getJobParameters called with jobType='{}'", jobType);

        if (jobType == null || jobType.isBlank()) {
            log.warn("jobType is empty");
            return paramInputs.data("parameters", List.of())
                    .data("existingValues", null);
        }

        try {
            List<JobParameter> parameters = getJobParametersUseCase.execute(jobType).stream().sorted(Comparator.comparing(JobParameter::name)).toList();
            log.info("Found {} parameters for job type '{}'", parameters.size(), jobType);
            for (JobParameter param : parameters) {
                log.debug("  - Parameter: {} (type: {})", param.name(), param.type());
            }
            return paramInputs.data("parameters", parameters)
                    .data("existingValues", null);
        } catch (Exception e) {
            log.error("Error getting parameters for job type '{}': {}", jobType, e.getMessage(), e);
            return paramInputs.data("parameters", List.of())
                    .data("existingValues", null);
        }
    }

    @POST
    @RolesAllowed({"configurator", "admin"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response createJob(
            @FormParam("jobType") String jobType,
            @FormParam("jobName") String jobName,
            @FormParam("triggerType") String triggerType,
            @FormParam("scheduledAt") String scheduledAt,
            MultivaluedMap<String, String> allFormParams,
            RoutingContext context) {
        // Validate required fields
        if (jobType == null || jobType.isBlank()) {
            log.warn("Job type is empty");
            return Response.ok(scheduledJobsTable.data("jobs", Collections.emptyList())).build();
        }

        boolean isExternalTrigger = "external".equals(triggerType);
        Instant scheduledTime = null;

        if (!isExternalTrigger && scheduledAt != null && !scheduledAt.isBlank()) {
            // Parse datetime-local format
            LocalDateTime ldt = LocalDateTime.parse(scheduledAt);
            scheduledTime = ldt.atZone(ZoneId.systemDefault()).toInstant();
        }

        Map<String, String> paramMap = allFormParams.keySet().stream()
                .collect(HashMap::new,
                        (map, key) -> map.put(key, allFormParams.getFirst(key)),
                        HashMap::putAll);
        // Create job
        // jobType is the name of the job definition (e.g., fully qualified class name)
        // jobName is the user-defined name for this job instance
        createJobUseCase.execute(jobType, jobName, paramMap, scheduledTime, isExternalTrigger);

        // Return updated table with header to close modal
        TemplateInstance table = getScheduledJobsTable(null, "all", 0, 10, "scheduledAt", "asc");
        return Response.ok(table)
                .header("HX-Trigger", "closeModal")
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"configurator", "admin"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response updateJob(
            @PathParam("id") UUID jobId,
            @FormParam("jobType") String jobType,
            @FormParam("jobName") String jobName,
            @FormParam("triggerType") String triggerType,
            @FormParam("scheduledAt") String scheduledAt,
            MultivaluedMap<String, String> allFormParams,
            RoutingContext context) {

        log.info("Updating job {} - jobType={}, jobName={}, triggerType={}, scheduledAt={}",
                jobId, jobType, jobName, triggerType, scheduledAt);
        log.info("All form parameters: {}", allFormParams);

        // Validate required fields
        if (jobType == null || jobType.isBlank()) {
            log.warn("Job type is empty");
            return Response.ok(scheduledJobsTable.data("jobs", Collections.emptyList())).build();
        }

        boolean isExternalTrigger = "external".equals(triggerType);
        Instant scheduledTime = null;

        if (!isExternalTrigger && scheduledAt != null && !scheduledAt.isBlank()) {
            // Parse datetime-local format
            LocalDateTime ldt = LocalDateTime.parse(scheduledAt);
            scheduledTime = ldt.atZone(ZoneId.systemDefault()).toInstant();
        }

        Map<String, String> paramMap = allFormParams.keySet().stream()
                .collect(HashMap::new,
                        (map, key) -> map.put(key, allFormParams.getFirst(key)),
                        HashMap::putAll);

        log.info("Parameter map before update: {}", paramMap);

        // Update job
        updateJobUseCase.execute(jobId, jobType, jobName, paramMap, scheduledTime, isExternalTrigger);

        // Return updated table with header to close modal
        TemplateInstance table = getScheduledJobsTable(null, "all", 0, 10, "scheduledAt", "asc");
        return Response.ok(table)
                .header("HX-Trigger", "closeModal")
                .build();
    }


    @DELETE
    @Path("/{id}")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance deleteJob(@PathParam("id") UUID jobId) {
        deleteJobUseCase.execute(jobId);
        return getScheduledJobsTable(null, "all", 0, 10, "scheduledAt", "asc");
    }

    @POST
    @Path("/{id}/execute")
    @RolesAllowed({"admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance executeJob(@PathParam("id") UUID jobId) {
        executeJobUseCase.execute(jobId);
        return getScheduledJobsTable(null, "all", 0, 10, "scheduledAt", "asc");
    }
}
