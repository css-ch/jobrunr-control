package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.application.scheduling.*;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.domain.ScheduledJobInfoView;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI Controller for scheduled jobs.
 * Renders type-safe Qute templates and processes HTMX requests.
 */
@Path("/q/jobrunr-control/scheduled")
public class ScheduledJobsController {

    private static final Logger log = Logger.getLogger(ScheduledJobsController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        public static native TemplateInstance scheduledJobs(List<String> availableJobTypes);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        public static native TemplateInstance scheduledJobsTable(List<ScheduledJobInfoView> jobs,
                                                                 Map<String, Object> pagination,
                                                                 List<TemplateExtensions.PageItem> pageRange,
                                                                 String search, String filter, String jobType,
                                                                 String sortBy, String sortOrder);

        public static native TemplateInstance paramInputs(List<JobParameter> parameters,
                                                          Map<String, Object> existingValues);
    }

    @CheckedTemplate(basePath = "modals", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Modals {
        public static native TemplateInstance jobForm(List<JobDefinition> jobDefinitions,
                                                      boolean isEdit,
                                                      ScheduledJobInfo job,
                                                      List<JobParameter> parameters);
    }

    @Inject
    DiscoverJobsUseCase discoverJobsUseCase;

    @Inject
    GetJobParametersUseCase getJobParametersUseCase;

    @Inject
    ResolveParametersUseCase resolveParametersUseCase;

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
    ExecuteScheduledJobUseCase executeScheduledJobUseCase;

    @Inject
    JobParameterValidator validator;

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getScheduledJobsView() {
        List<String> availableJobTypes = getAvailableJobTypes();
        return Templates.scheduledJobs(availableJobTypes);
    }

    @GET
    @Path("/table")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getScheduledJobsTable(
            @QueryParam("search") String search,
            @QueryParam("filter") @DefaultValue("all") String filter,
            @QueryParam("jobType") String jobType,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("scheduledAt") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {

        List<ScheduledJobInfo> jobs = getScheduledJobsUseCase.execute();

        // Exclude template jobs from scheduled jobs view
        jobs = jobs.stream()
                .filter(job -> !job.isTemplate())
                .toList();

        // Filter by trigger type
        if ("external".equals(filter)) {
            jobs = jobs.stream()
                    .filter(ScheduledJobInfo::isExternallyTriggerable)
                    .toList();
        } else if ("scheduled".equals(filter)) {
            jobs = jobs.stream()
                    .filter(j -> !j.isExternallyTriggerable())
                    .toList();
        }

        // Filter by job type if specified
        if (jobType != null && !jobType.isBlank() && !"all".equals(jobType)) {
            jobs = jobs.stream()
                    .filter(job -> jobType.equals(job.getJobType()))
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
        PaginationHelper.PaginationResult<ScheduledJobInfo> paginationResult = PaginationHelper.paginate(jobs, page, size);

        // Convert to view models with resolved parameters
        List<ScheduledJobInfoView> jobViews = paginationResult.getPageItems().stream()
                .map(this::toView)
                .collect(Collectors.toList());

        return Components.scheduledJobsTable(
                jobViews,
                paginationResult.getMetadata(),
                paginationResult.getPageRange(),
                search != null ? search : "",
                filter,
                jobType != null ? jobType : "all",
                sortBy,
                sortOrder
        );
    }


    @GET
    @Path("/modal/new")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getNewJobModal() {
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions();

        return Modals.jobForm(jobDefinitions, false, null, null);
    }

    @GET
    @Path("/modal/{id}/edit")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getEditJobModal(@PathParam("id") UUID jobId) {
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions();

        ScheduledJobInfo jobInfo = getScheduledJobByIdUseCase.execute(jobId)
                .orElseThrow(() -> new NotFoundException("Job nicht gefunden: " + jobId));

        // Resolve parameters (expand external parameter sets)
        Map<String, Object> resolvedParameters = resolveParametersUseCase.execute(jobInfo.getParameters());

        // Create a new ScheduledJobInfo with resolved parameters for the form
        ScheduledJobInfo jobInfoWithResolvedParams = new ScheduledJobInfo(
                jobInfo.getJobId(),
                jobInfo.getJobName(),
                jobInfo.getJobDefinition(),
                jobInfo.getScheduledAt(),
                resolvedParameters,
                jobInfo.isExternallyTriggerable(),
                jobInfo.getLabels()
        );

        // Load parameter definitions for this job type
        List<JobParameter> parameters = Collections.emptyList();
        try {
            parameters = getJobParametersUseCase.execute(jobInfo.getJobType());
            log.infof("Loaded %s parameter definitions for job type '%s' in edit mode", parameters.size(), jobInfo.getJobType());
        } catch (Exception e) {
            log.errorf("Error loading parameters for job type '%s': %s", jobInfo.getJobType(), e.getMessage(), e);
        }

        return Modals.jobForm(jobDefinitions, true, jobInfoWithResolvedParams, parameters);
    }

    @GET
    @Path("/modal/parameters")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getJobParameters(@QueryParam("jobType") String jobType) {
        log.debugf("getJobParameters called with jobType='%s'", jobType);

        if (jobType == null || jobType.isBlank()) {
            log.warnf("jobType is empty");
            return Components.paramInputs(List.of(), null);
        }

        try {
            List<JobParameter> parameters = getJobParametersUseCase.execute(jobType).stream().sorted(Comparator.comparing(JobParameter::name)).toList();
            log.infof("Found %s parameters for job type '%s'", parameters.size(), jobType);
            for (JobParameter param : parameters) {
                log.infof("  - Parameter: %s (type: %s, required: %s, defaultValue: '%s')",
                        param.name(), param.type(), param.required(), param.defaultValue());
            }
            return Components.paramInputs(parameters, null);
        } catch (Exception e) {
            log.errorf("Error getting parameters for job type '%s': %s", jobType, e.getMessage(), e);
            return Components.paramInputs(List.of(), null);
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
        try {
            // Validate required fields
            if (jobType == null || jobType.isBlank()) {
                log.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }

            Map<String, String> paramMap = extractParameterMap(allFormParams);
            boolean isExternalTrigger = "external".equals(triggerType);
            Instant scheduledTime = isExternalTrigger ? null : parseScheduledTime(scheduledAt);

            // Create job
            // jobType is the name of the job definition (e.g., fully qualified class name)
            // jobName is the user-defined name for this job instance
            createJobUseCase.execute(jobType, jobName, paramMap, scheduledTime, isExternalTrigger);

            // Return updated table with header to close modal
            return buildModalCloseResponse(getDefaultScheduledJobsTable());
        } catch (Exception e) {
            log.errorf(e, "Error creating job");
            return buildErrorResponse("Error creating job: " + e.getMessage());
        }
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

        try {
            log.infof("Updating job %s - jobType=%s, jobName=%s, triggerType=%s, scheduledAt=%s",
                    jobId, jobType, jobName, triggerType, scheduledAt);
            log.infof("All form parameters: %s", allFormParams);

            // Validate required fields
            if (jobType == null || jobType.isBlank()) {
                log.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }

            boolean isExternalTrigger = "external".equals(triggerType);
            Instant scheduledTime = isExternalTrigger ? null : parseScheduledTime(scheduledAt);

            Map<String, String> paramMap = extractParameterMap(allFormParams);

            log.infof("Parameter map before update: %s", paramMap);

            // Update job
            updateJobUseCase.execute(jobId, jobType, jobName, paramMap, scheduledTime, isExternalTrigger);

            // Return updated table with header to close modal
            return buildModalCloseResponse(getDefaultScheduledJobsTable());
        } catch (Exception e) {
            log.errorf(e, "Error updating job %s", jobId);
            return buildErrorResponse("Error updating job: " + e.getMessage());
        }
    }


    @DELETE
    @Path("/{id}")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance deleteJob(@PathParam("id") UUID jobId) {
        deleteJobUseCase.execute(jobId);
        return getDefaultScheduledJobsTable();
    }

    @POST
    @Path("/{id}/execute")
    @RolesAllowed({"admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance executeJob(@PathParam("id") UUID jobId) {
        executeScheduledJobUseCase.execute(jobId);
        return getDefaultScheduledJobsTable();
    }

    private Comparator<ScheduledJobInfo> getComparator(String sortBy) {
        return switch (sortBy) {
            case "jobName" -> Comparator.comparing(ScheduledJobInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
            case "jobType" -> Comparator.comparing(ScheduledJobInfo::getJobType, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(ScheduledJobInfo::getScheduledAt);
        };
    }

    private List<JobDefinition> getSortedJobDefinitions() {
        return discoverJobsUseCase.execute().stream()
                .sorted(Comparator.comparing(JobDefinition::jobType))
                .toList();
    }

    private List<String> getAvailableJobTypes() {
        // Get all available job types from job definitions
        return discoverJobsUseCase.execute().stream()
                .map(JobDefinition::jobType)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Instant parseScheduledTime(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isBlank()) {
            return null;
        }
        LocalDateTime ldt = LocalDateTime.parse(scheduledAt);
        return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }

    private Map<String, String> extractParameterMap(MultivaluedMap<String, String> allFormParams) {
        return allFormParams.keySet().stream()
                .collect(HashMap::new,
                        (map, key) -> {
                            List<String> values = allFormParams.get(key);
                            if (values != null && values.size() > 1) {
                                // Multiple values (e.g., from multiselect) - join with comma
                                map.put(key, String.join(",", values));
                            } else {
                                // Single value
                                map.put(key, allFormParams.getFirst(key));
                            }
                        },
                        HashMap::putAll);
    }

    private TemplateInstance getDefaultScheduledJobsTable() {
        return getScheduledJobsTable(null, "all", null, 0, 10, "scheduledAt", "asc");
    }

    private Response buildModalCloseResponse(TemplateInstance table) {
        return Response.ok(table)
                .header("HX-Trigger", "closeModal")
                .build();
    }

    /**
     * Builds an error response that displays in the modal's alert area.
     * The modal stays open so the user can fix the error.
     * Uses HTMX out-of-band (OOB) swap to reliably target the alert container.
     */
    private Response buildErrorResponse(String errorMessage) {
        String errorHtml = String.format(
                "<div id=\"form-alerts\" hx-swap-oob=\"true\">" +
                        "<div class=\"alert alert-danger alert-dismissible fade show\" role=\"alert\">" +
                        "<i class=\"bi bi-exclamation-triangle-fill\"></i> <strong>Error:</strong> %s" +
                        "<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"alert\" aria-label=\"Close\"></button>" +
                        "</div>" +
                        "</div>",
                errorMessage
        );
        return Response.ok(errorHtml)
                .header("HX-Trigger", "scrollToError")
                .build();
    }

    /**
     * Converts ScheduledJobInfo to ScheduledJobInfoView with resolved parameters.
     * If the job uses external parameter storage, the parameters are loaded from the parameter set.
     * Parameters are truncated to avoid sending large data in the list view.
     */
    private ScheduledJobInfoView toView(ScheduledJobInfo jobInfo) {
        boolean usesExternal = resolveParametersUseCase.usesExternalStorage(jobInfo.getParameters());
        Map<String, Object> resolvedParameters = resolveParametersUseCase.execute(jobInfo.getParameters());

        // Truncate large parameter values to prevent 413 (Request Entity Too Large) errors
        Map<String, Object> truncatedParameters = truncateParameterValues(resolvedParameters);

        return ScheduledJobInfoView.from(jobInfo, truncatedParameters, usesExternal);
    }

    /**
     * Truncates large parameter values to prevent HTTP 413 errors.
     * String values longer than 1000 characters are truncated.
     * Collection/Map sizes are limited to prevent excessive data transfer.
     */
    private Map<String, Object> truncateParameterValues(Map<String, Object> parameters) {
        final int MAX_STRING_LENGTH = 1000;
        final int MAX_COLLECTION_SIZE = 100;

        Map<String, Object> truncated = new HashMap<>();

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String str) {
                if (str.length() > MAX_STRING_LENGTH) {
                    truncated.put(entry.getKey(), str.substring(0, MAX_STRING_LENGTH) + "... [truncated]");
                } else {
                    truncated.put(entry.getKey(), value);
                }
            } else if (value instanceof java.util.Collection<?> collection) {
                if (collection.size() > MAX_COLLECTION_SIZE) {
                    truncated.put(entry.getKey(), String.format("[Collection with %d items - too large to display]", collection.size()));
                } else {
                    truncated.put(entry.getKey(), value);
                }
            } else if (value instanceof Map<?, ?> map) {
                if (map.size() > MAX_COLLECTION_SIZE) {
                    truncated.put(entry.getKey(), String.format("[Map with %d entries - too large to display]", map.size()));
                } else {
                    truncated.put(entry.getKey(), value);
                }
            } else {
                truncated.put(entry.getKey(), value);
            }
        }

        return truncated;
    }

}
