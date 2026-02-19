package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.application.scheduling.StartJobUseCase;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UI Controller for template jobs.
 * Renders type-safe Qute templates and processes HTMX requests.
 * Template jobs are jobs with the "template" label that cannot be executed directly.
 */
@Path("/q/jobrunr-control/templates")
public class TemplatesController extends BaseController {

    private static final Logger LOG = Logger.getLogger(TemplatesController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        private Templates() {
            // Utility class
        }

        public static native TemplateInstance templates(List<String> availableJobTypes);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        private Components() {
            // Utility class
        }

        public static native TemplateInstance templatesTable(List<ScheduledJobInfoView> jobs,
                                                             PaginationHelper.PaginationMetadata pagination,
                                                             List<TemplateExtensions.PageItem> pageRange,
                                                             String search,
                                                             String jobType,
                                                             String sortBy, String sortOrder);
    }

    @CheckedTemplate(basePath = "modals", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Modals {
        private Modals() {
            // Utility class
        }

        public static native TemplateInstance templateForm(List<JobDefinition> jobDefinitions,
                                                           boolean isEdit,
                                                           ScheduledJobInfo job,
                                                           List<JobParameter> parameters);
    }

    private final DiscoverJobsUseCase discoverJobsUseCase;
    private final GetJobParametersUseCase getJobParametersUseCase;
    private final ResolveParametersUseCase resolveParametersUseCase;
    private final GetTemplatesUseCase getTemplatesUseCase;
    private final GetTemplateByIdUseCase getTemplateByIdUseCase;
    private final CreateTemplateUseCase createTemplateUseCase;
    private final UpdateTemplateUseCase updateTemplateUseCase;
    private final DeleteTemplateUseCase deleteTemplateUseCase;
    private final CloneTemplateUseCase cloneTemplateUseCase;
    private final StartJobUseCase startJobUseCase;

    @Inject
    public TemplatesController(
            DiscoverJobsUseCase discoverJobsUseCase,
            GetJobParametersUseCase getJobParametersUseCase,
            ResolveParametersUseCase resolveParametersUseCase,
            GetTemplatesUseCase getTemplatesUseCase,
            GetTemplateByIdUseCase getTemplateByIdUseCase,
            CreateTemplateUseCase createTemplateUseCase,
            UpdateTemplateUseCase updateTemplateUseCase,
            DeleteTemplateUseCase deleteTemplateUseCase,
            CloneTemplateUseCase cloneTemplateUseCase,
            StartJobUseCase startJobUseCase) {
        this.discoverJobsUseCase = discoverJobsUseCase;
        this.getJobParametersUseCase = getJobParametersUseCase;
        this.resolveParametersUseCase = resolveParametersUseCase;
        this.getTemplatesUseCase = getTemplatesUseCase;
        this.getTemplateByIdUseCase = getTemplateByIdUseCase;
        this.createTemplateUseCase = createTemplateUseCase;
        this.updateTemplateUseCase = updateTemplateUseCase;
        this.deleteTemplateUseCase = deleteTemplateUseCase;
        this.cloneTemplateUseCase = cloneTemplateUseCase;
        this.startJobUseCase = startJobUseCase;
    }

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTemplatesView() {
        List<String> availableJobTypes = getAvailableJobTypes(discoverJobsUseCase);
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

        // Use base controller helper for filter, search, sort, paginate
        PaginationHelper.PaginationResult<ScheduledJobInfo> paginationResult =
                filterSortAndPaginate(jobs, jobType, search, sortBy, sortOrder, page, size, this::getComparator);

        // Convert to view models with resolved parameters
        List<ScheduledJobInfoView> jobViews = paginationResult.pageItems().stream()
                .map(job -> toView(job, resolveParametersUseCase))
                .toList();

        return Components.templatesTable(
                jobViews,
                paginationResult.metadata(),
                paginationResult.pageRange(),
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
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions(discoverJobsUseCase);
        return Modals.templateForm(jobDefinitions, false, null, null);
    }

    @GET
    @Path("/modal/{id}/edit")
    @RolesAllowed({"configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getEditTemplateModal(@PathParam("id") UUID jobId) {
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions(discoverJobsUseCase);

        ScheduledJobInfo jobInfo = getTemplateByIdUseCase.execute(jobId)
                .orElseThrow(() -> new NotFoundException("Template nicht gefunden: " + jobId));

        // Use base controller helper to resolve parameters
        ResolvedJobData resolvedData = resolveJobParameters(
                jobInfo,
                resolveParametersUseCase::execute,
                getJobParametersUseCase::execute
        );

        return Modals.templateForm(jobDefinitions, true,
                resolvedData.jobInfoWithResolvedParams, resolvedData.parameters);
    }

    @GET
    @Path("/modal/parameters")
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getJobParameters(@QueryParam("jobType") String jobType) {
        LOG.infof("getJobParameters called with jobType='%s'", jobType);

        if (jobType == null || jobType.isBlank()) {
            LOG.warnf("jobType is empty");
            return ScheduledJobsController.Components.paramInputs(List.of(), null);
        }

        try {
            List<JobParameter> parameters = getJobParametersUseCase.execute(jobType).stream()
                    .sorted(Comparator.comparing(JobParameter::order))
                    .toList();
            return ScheduledJobsController.Components.paramInputs(parameters, null);
        } catch (Exception e) {
            LOG.errorf(e, "Error getting parameters for job type '%s'", jobType);
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
                LOG.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }

            Map<String, String> paramMap = extractParameterMap(allFormParams);

            // Create template job
            createTemplateUseCase.execute(jobType, jobName, paramMap);

            return buildModalCloseResponse(getDefaultTemplatesTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error creating template");
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
            LOG.debugf("Updating template %s - jobType=%s, jobName=%s", jobId, jobType, jobName);
            if (jobType == null || jobType.isBlank()) {
                LOG.warnf("Job type is empty");
                return buildErrorResponse("Job type is required");
            }
            Map<String, String> paramMap = extractParameterMap(allFormParams);
            // Update template job
            updateTemplateUseCase.execute(jobId, jobType, jobName, paramMap);
            return buildModalCloseResponse(getDefaultTemplatesTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error updating template %s", jobId);
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance cloneTemplate(@PathParam("id") UUID jobId) {
        cloneTemplateUseCase.execute(jobId, null);
        return getDefaultTemplatesTable();
    }

    @POST
    @Path("/{id}/start")
    @RolesAllowed({"admin"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance startTemplate(@PathParam("id") UUID templateId) {
        startJobUseCase.execute(templateId, null, null, false);
        return getDefaultTemplatesTable();
    }

    private Comparator<ScheduledJobInfo> getComparator(String sortBy) {
        if ("jobType".equals(sortBy)) {
            return Comparator.comparing(ScheduledJobInfo::getJobType, String.CASE_INSENSITIVE_ORDER);
        } else {
            return Comparator.comparing(ScheduledJobInfo::getJobName, String.CASE_INSENSITIVE_ORDER);
        }
    }


    private TemplateInstance getDefaultTemplatesTable() {
        return getTemplatesTable(null, "all", 0, 10, "jobName", "asc");
    }
}
