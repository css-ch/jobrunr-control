package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.application.template.*;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.domain.ScheduledJobInfoView;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UI Controller for template jobs.
 * Renders type-safe Qute templates and processes HTMX requests.
 * Template jobs are jobs with the "template" label that cannot be executed directly.
 */
@Path("/q/jobrunr-control/templates")
public class TemplatesController extends BaseController {

    private static final Logger log = Logger.getLogger(TemplatesController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        public static native TemplateInstance templates(List<String> availableJobTypes);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        public static native TemplateInstance templatesTable(List<ScheduledJobInfoView> jobs,
                                                             Map<String, Object> pagination,
                                                             List<TemplateExtensions.PageItem> pageRange,
                                                             String search,
                                                             String jobType,
                                                             String sortBy, String sortOrder);
    }

    @CheckedTemplate(basePath = "modals", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Modals {
        public static native TemplateInstance templateForm(List<JobDefinition> jobDefinitions,
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
    GetTemplatesUseCase getTemplatesUseCase;

    @Inject
    GetTemplateByIdUseCase getTemplateByIdUseCase;

    @Inject
    CreateTemplateUseCase createTemplateUseCase;

    @Inject
    UpdateTemplateUseCase updateTemplateUseCase;

    @Inject
    DeleteTemplateUseCase deleteTemplateUseCase;

    @Inject
    CloneTemplateUseCase cloneTemplateUseCase;

    @Inject
    ExecuteTemplateUseCase executeTemplateUseCase;

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTemplatesView() {
        List<String> availableJobTypes = getAvailableJobTypes();
        return Templates.templates(availableJobTypes);
    }

    @GET
    @Path("/table")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTemplatesTable(
            @QueryParam("search") String search,
            @QueryParam("jobType") String jobType,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("jobName") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {

        // Get all template jobs
        List<ScheduledJobInfo> jobs = getTemplatesUseCase.execute();

        // Filter by job type if specified
        if (jobType != null && !jobType.isBlank() && !"all".equals(jobType)) {
            jobs = jobs.stream()
                    .filter(job -> jobType.equals(job.getJobType()))
                    .toList();
        }

        // Apply search
        jobs = JobSearchUtils.applySearchToScheduledJobs(search, jobs);

        // Apply sorting
        Comparator<ScheduledJobInfo> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        jobs = jobs.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        // Apply pagination
        PaginationHelper.PaginationResult<ScheduledJobInfo> paginationResult = PaginationHelper.paginate(jobs, page, size);

        // Convert to view models with resolved parameters
        List<ScheduledJobInfoView> jobViews = paginationResult.getPageItems().stream()
                .map(this::toView)
                .collect(Collectors.toList());

        return Components.templatesTable(
                jobViews,
                paginationResult.getMetadata(),
                paginationResult.getPageRange(),
                search != null ? search : "",
                jobType != null ? jobType : "all",
                sortBy,
                sortOrder
        );
    }

    @GET
    @Path("/modal/new")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getNewTemplateModal() {
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions();
        return Modals.templateForm(jobDefinitions, false, null, null);
    }

    @GET
    @Path("/modal/{id}/edit")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getEditTemplateModal(@PathParam("id") UUID jobId) {
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions();

        ScheduledJobInfo jobInfo = getTemplateByIdUseCase.execute(jobId)
                .orElseThrow(() -> new NotFoundException("Template nicht gefunden: " + jobId));

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

        return Modals.templateForm(jobDefinitions, true, jobInfoWithResolvedParams, parameters);
    }

    @GET
    @Path("/modal/parameters")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getJobParameters(@QueryParam("jobType") String jobType) {
        log.infof("getJobParameters called with jobType='%s'", jobType);

        if (jobType == null || jobType.isBlank()) {
            log.warnf("jobType is empty");
            return ScheduledJobsController.Components.paramInputs(List.of(), null);
        }

        try {
            List<JobParameter> parameters = getJobParametersUseCase.execute(jobType).stream()
                    .sorted(Comparator.comparing(JobParameter::name))
                    .toList();
            log.infof("Found %s parameters for job type '%s'", parameters.size(), jobType);
            return ScheduledJobsController.Components.paramInputs(parameters, null);
        } catch (Exception e) {
            log.errorf("Error getting parameters for job type '%s': %s", jobType, e.getMessage(), e);
            return ScheduledJobsController.Components.paramInputs(List.of(), null);
        }
    }

    @POST
    @RolesAllowed({"configurator", "admin"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response createTemplate(
            @FormParam("jobType") String jobType,
            @FormParam("jobName") String jobName,
            MultivaluedMap<String, String> allFormParams) {

        try {
            if (jobType == null || jobType.isBlank()) {
                log.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }

            Map<String, String> paramMap = extractParameterMap(allFormParams);

            // Create template job
            createTemplateUseCase.execute(jobType, jobName, paramMap);

            return buildModalCloseResponse(getDefaultTemplatesTable());
        } catch (Exception e) {
            log.errorf(e, "Error creating template");
            return buildErrorResponse("Error creating template: " + e.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"configurator", "admin"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response updateTemplate(
            @PathParam("id") UUID jobId,
            @FormParam("jobType") String jobType,
            @FormParam("jobName") String jobName,
            MultivaluedMap<String, String> allFormParams) {

        try {
            log.infof("Updating template %s - jobType=%s, jobName=%s", jobId, jobType, jobName);

            if (jobType == null || jobType.isBlank()) {
                log.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }

            Map<String, String> paramMap = extractParameterMap(allFormParams);

            // Update template job
            updateTemplateUseCase.execute(jobId, jobType, jobName, paramMap);

            return buildModalCloseResponse(getDefaultTemplatesTable());
        } catch (Exception e) {
            log.errorf(e, "Error updating template %s", jobId);
            return buildErrorResponse("Error updating template: " + e.getMessage());
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance deleteTemplate(@PathParam("id") UUID jobId) {
        deleteTemplateUseCase.execute(jobId);
        return getDefaultTemplatesTable();
    }

    @POST
    @Path("/{id}/clone")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance cloneTemplate(@PathParam("id") UUID jobId) {
        cloneTemplateUseCase.execute(jobId, null);
        return getDefaultTemplatesTable();
    }

    @POST
    @Path("/{id}/start")
    @RolesAllowed({"admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance startTemplate(@PathParam("id") UUID templateId) {
        executeTemplateUseCase.execute(templateId, null, null);
        return getDefaultTemplatesTable();
    }


    private Comparator<ScheduledJobInfo> getComparator(String sortBy) {
        return switch (sortBy) {
            case "jobName" -> Comparator.comparing(ScheduledJobInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
            case "jobType" -> Comparator.comparing(ScheduledJobInfo::getJobType, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(ScheduledJobInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
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

    private TemplateInstance getDefaultTemplatesTable() {
        return getTemplatesTable(null, null, 0, 10, "jobName", "asc");
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
