package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.application.scheduling.*;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UI Controller for scheduled jobs.
 * Renders type-safe Qute templates and processes HTMX requests.
 */
@Path("/q/jobrunr-control/scheduled")
@SuppressWarnings("java:S1192") // "external" filter literal duplication is acceptable for query param clarity
public class ScheduledJobsController extends BaseController {

    private static final Logger LOG = Logger.getLogger(ScheduledJobsController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {

        private Templates() {
            // Qute class
        }

        public static native TemplateInstance scheduledJobs(List<String> availableJobTypes);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {

        private Components() {
            // Qute class
        }

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

        private Modals() {
            // Qute class
        }

        public static native TemplateInstance jobForm(List<JobDefinition> jobDefinitions,
                                                      boolean isEdit,
                                                      ScheduledJobInfo job,
                                                      List<JobParameter> parameters);
    }

    private final DiscoverJobsUseCase discoverJobsUseCase;
    private final GetJobParametersUseCase getJobParametersUseCase;
    private final ResolveParametersUseCase resolveParametersUseCase;
    private final GetScheduledJobsUseCase getScheduledJobsUseCase;
    private final GetScheduledJobByIdUseCase getScheduledJobByIdUseCase;
    private final CreateScheduledJobUseCase createJobUseCase;
    private final UpdateScheduledJobUseCase updateJobUseCase;
    private final DeleteScheduledJobUseCase deleteJobUseCase;
    private final ExecuteScheduledJobUseCase executeScheduledJobUseCase;

    @Inject
    public ScheduledJobsController(
            DiscoverJobsUseCase discoverJobsUseCase,
            GetJobParametersUseCase getJobParametersUseCase,
            ResolveParametersUseCase resolveParametersUseCase,
            GetScheduledJobsUseCase getScheduledJobsUseCase,
            GetScheduledJobByIdUseCase getScheduledJobByIdUseCase,
            CreateScheduledJobUseCase createJobUseCase,
            UpdateScheduledJobUseCase updateJobUseCase,
            DeleteScheduledJobUseCase deleteJobUseCase,
            ExecuteScheduledJobUseCase executeScheduledJobUseCase) {
        this.discoverJobsUseCase = discoverJobsUseCase;
        this.getJobParametersUseCase = getJobParametersUseCase;
        this.resolveParametersUseCase = resolveParametersUseCase;
        this.getScheduledJobsUseCase = getScheduledJobsUseCase;
        this.getScheduledJobByIdUseCase = getScheduledJobByIdUseCase;
        this.createJobUseCase = createJobUseCase;
        this.updateJobUseCase = updateJobUseCase;
        this.deleteJobUseCase = deleteJobUseCase;
        this.executeScheduledJobUseCase = executeScheduledJobUseCase;

    }

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

        // Use base controller helper for filter, search, sort, paginate
        PaginationHelper.PaginationResult<ScheduledJobInfo> paginationResult =
                filterSortAndPaginate(jobs, jobType, search, sortBy, sortOrder, page, size, this::getComparator);

        // Convert to view models with resolved parameters
        List<ScheduledJobInfoView> jobViews = paginationResult.getPageItems().stream()
                .map(this::toView)
                .toList();

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
        return Modals.jobForm(getSortedJobDefinitions(), false, null, null);
    }

    @GET
    @Path("/modal/{id}/edit")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getEditJobModal(@PathParam("id") UUID jobId) {
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions();

        ScheduledJobInfo jobInfo = getScheduledJobByIdUseCase.execute(jobId)
                .orElseThrow(() -> new NotFoundException("Job nicht gefunden: " + jobId));

        // Use base controller helper to resolve parameters
        ResolvedJobData resolvedData = resolveJobParameters(
                jobInfo,
                resolveParametersUseCase::execute,
                getJobParametersUseCase::execute
        );

        return Modals.jobForm(jobDefinitions, true,
                resolvedData.jobInfoWithResolvedParams, resolvedData.parameters);
    }

    @GET
    @Path("/modal/parameters")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getJobParameters(@QueryParam("jobType") String jobType) {
        LOG.debugf("getJobParameters called with jobType='%s'", jobType);

        if (jobType == null || jobType.isBlank()) {
            LOG.warnf("jobType is empty");
            return Components.paramInputs(List.of(), null);
        }

        try {
            List<JobParameter> parameters = getJobParametersUseCase.execute(jobType).stream().sorted(Comparator.comparing(JobParameter::name)).toList();
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Found %s parameters for job type '%s'", parameters.size(), jobType);
                for (JobParameter param : parameters) {
                    LOG.debugf("  - Parameter: %s (type: %s, required: %s, defaultValue: '%s')",
                            param.name(), param.type(), param.required(), param.defaultValue());
                }
            }
            return Components.paramInputs(parameters, null);
        } catch (Exception e) {
            LOG.errorf("Error getting parameters for job type '%s': %s", jobType, e.getMessage(), e);
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
                LOG.warnf("Job type is empty");
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
            LOG.errorf(e, "Error creating job");
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
            LOG.infof("Updating job %s - jobType=%s, jobName=%s, triggerType=%s, scheduledAt=%s",
                    jobId, jobType, jobName, triggerType, scheduledAt);
            LOG.debugf("All form parameters: %s", allFormParams);

            // Validate required fields
            if (jobType == null || jobType.isBlank()) {
                LOG.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }

            boolean isExternalTrigger = "external".equals(triggerType);
            Instant scheduledTime = isExternalTrigger ? null : parseScheduledTime(scheduledAt);

            Map<String, String> paramMap = extractParameterMap(allFormParams);
            // Update job
            updateJobUseCase.execute(jobId, jobType, jobName, paramMap, scheduledTime, isExternalTrigger);
            // Return updated table with header to close modal
            return buildModalCloseResponse(getDefaultScheduledJobsTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error updating job %s", jobId);
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
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

    private TemplateInstance getDefaultScheduledJobsTable() {
        return getScheduledJobsTable(null, "all", null, 0, 10, "scheduledAt", "asc");
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


}
