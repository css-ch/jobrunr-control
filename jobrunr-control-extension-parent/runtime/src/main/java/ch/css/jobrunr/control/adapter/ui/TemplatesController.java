package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.application.scheduling.CreateScheduledJobUseCase;
import ch.css.jobrunr.control.application.scheduling.DeleteScheduledJobUseCase;
import ch.css.jobrunr.control.application.scheduling.GetScheduledJobByIdUseCase;
import ch.css.jobrunr.control.application.scheduling.UpdateScheduledJobUseCase;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
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
public class TemplatesController {

    private static final Logger log = Logger.getLogger(TemplatesController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        public static native TemplateInstance templates();
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        public static native TemplateInstance templatesTable(List<ScheduledJobInfo> jobs,
                                                             Map<String, Object> pagination,
                                                             List<TemplateExtensions.PageItem> pageRange,
                                                             String search,
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
    GetScheduledJobsUseCase getScheduledJobsUseCase;

    @Inject
    GetScheduledJobByIdUseCase getScheduledJobByIdUseCase;

    @Inject
    CreateScheduledJobUseCase createJobUseCase;

    @Inject
    UpdateScheduledJobUseCase updateJobUseCase;

    @Inject
    DeleteScheduledJobUseCase deleteJobUseCase;

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTemplatesView() {
        return Templates.templates();
    }

    @GET
    @Path("/table")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTemplatesTable(
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("jobName") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {

        // Get all scheduled jobs and filter for templates
        List<ScheduledJobInfo> jobs = getScheduledJobsUseCase.execute().stream()
                .filter(this::isTemplateJob)
                .toList();

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

        return Components.templatesTable(
                paginationResult.getPageItems(),
                paginationResult.getMetadata(),
                paginationResult.getPageRange(),
                search != null ? search : "",
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

        ScheduledJobInfo jobInfo = getScheduledJobByIdUseCase.execute(jobId)
                .orElseThrow(() -> new NotFoundException("Template nicht gefunden: " + jobId));

        // Load parameter definitions for this job type
        List<JobParameter> parameters = Collections.emptyList();
        try {
            parameters = getJobParametersUseCase.execute(jobInfo.getJobType());
            log.infof("Loaded %s parameter definitions for job type '%s' in edit mode", parameters.size(), jobInfo.getJobType());
        } catch (Exception e) {
            log.errorf("Error loading parameters for job type '%s': %s", jobInfo.getJobType(), e.getMessage(), e);
        }

        return Modals.templateForm(jobDefinitions, true, jobInfo, parameters);
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

        if (jobType == null || jobType.isBlank()) {
            log.warnf("Job type is empty");
            return Response.ok(getDefaultTemplatesTable()).build();
        }

        Map<String, String> paramMap = extractParameterMap(allFormParams);

        // Create template job - always external trigger, with "template" label
        createJobUseCase.execute(jobType, jobName, paramMap, null, true, java.util.List.of("template"));

        return buildModalCloseResponse(getDefaultTemplatesTable());
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

        log.infof("Updating template %s - jobType=%s, jobName=%s", jobId, jobType, jobName);

        if (jobType == null || jobType.isBlank()) {
            log.warnf("Job type is empty");
            return Response.ok(getDefaultTemplatesTable()).build();
        }

        Map<String, String> paramMap = extractParameterMap(allFormParams);

        // Update template job - always external trigger, maintain "template" label
        updateJobUseCase.execute(jobId, jobType, jobName, paramMap, null, true, java.util.List.of("template"));

        return buildModalCloseResponse(getDefaultTemplatesTable());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance deleteTemplate(@PathParam("id") UUID jobId) {
        deleteJobUseCase.execute(jobId);
        return getDefaultTemplatesTable();
    }

    private boolean isTemplateJob(ScheduledJobInfo job) {
        // Template jobs have the "template" label
        return job.isTemplate();
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

    private Map<String, String> extractParameterMap(MultivaluedMap<String, String> allFormParams) {
        return allFormParams.keySet().stream()
                .collect(HashMap::new,
                        (map, key) -> map.put(key, allFormParams.getFirst(key)),
                        HashMap::putAll);
    }

    private TemplateInstance getDefaultTemplatesTable() {
        return getTemplatesTable(null, 0, 10, "jobName", "asc");
    }

    private Response buildModalCloseResponse(TemplateInstance table) {
        return Response.ok(table)
                .header("HX-Trigger", "closeModal")
                .build();
    }
}
