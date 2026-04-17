package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.application.scheduling.StartJobUseCase;
import ch.css.jobrunr.control.application.template.*;
import ch.css.jobrunr.control.domain.*;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
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
@ApplicationScoped
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
                                                             String sortBy, String sortOrder,
                                                             boolean showUuid);
    }

    @CheckedTemplate(basePath = "modals", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Modals {
        private Modals() {
            // Utility class
        }

        public static native TemplateInstance templateForm(List<JobDefinition> jobDefinitions,
                                                           boolean isEdit,
                                                           ScheduledJobInfo job,
                                                           List<JobParameter> parameters,
                                                           List<JobParameterSection> parameterSections);
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
    private final JobRunrControlUiConfig uiConfig;

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
            StartJobUseCase startJobUseCase,
            JobRunrControlUiConfig uiConfig) {
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
        this.uiConfig = uiConfig;
    }

    public void handleIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        List<String> availableJobTypes = getAvailableJobTypes(discoverJobsUseCase);
        UiRoutingSupport.renderHtml(ctx, Templates.templates(availableJobTypes));
    }

    public void handleTable(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildTemplatesTable(
                UiRoutingSupport.queryParam(ctx, "search"),
                UiRoutingSupport.queryParam(ctx, "jobType"),
                UiRoutingSupport.intQueryParam(ctx, "page", 0),
                UiRoutingSupport.intQueryParam(ctx, "size", 10),
                UiRoutingSupport.queryParam(ctx, "sortBy", "jobName"),
                UiRoutingSupport.queryParam(ctx, "sortOrder", "asc")));
    }

    public void handleNewModal(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions(discoverJobsUseCase);
        UiRoutingSupport.renderHtml(ctx, Modals.templateForm(jobDefinitions, false, null, null, null));
    }

    public void handleEditModal(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions(discoverJobsUseCase);

        ScheduledJobInfo jobInfo = getTemplateByIdUseCase.execute(jobId)
                .orElse(null);
        if (jobInfo == null) {
            ctx.fail(404);
            return;
        }

        ResolvedJobData resolvedData = resolveJobParameters(
                jobInfo,
                resolveParametersUseCase::execute,
                getJobParametersUseCase::execute
        );

        UiRoutingSupport.renderHtml(ctx, Modals.templateForm(jobDefinitions, true,
                resolvedData.jobInfoWithResolvedParams, resolvedData.parameters, resolvedData.parameterSections));
    }

    public void handleParametersModal(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobType = UiRoutingSupport.queryParam(ctx, "jobType");
        LOG.infof("handleParametersModal jobType='%s'", jobType);

        if (jobType == null || jobType.isBlank()) {
            LOG.warnf("jobType is empty");
            UiRoutingSupport.renderHtml(ctx,
                    ScheduledJobsController.Components.paramInputs(List.of(), List.of(), null));
            return;
        }

        try {
            GetJobParametersUseCase.Result result = getJobParametersUseCase.execute(jobType);
            UiRoutingSupport.renderHtml(ctx,
                    ScheduledJobsController.Components.paramInputs(result.parameters(), result.parameterSections(), null));
        } catch (Exception e) {
            LOG.errorf(e, "Error getting parameters for job type '%s'", jobType);
            UiRoutingSupport.renderHtml(ctx,
                    ScheduledJobsController.Components.paramInputs(List.of(), List.of(), null));
        }
    }

    public void handleCreate(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        try {
            String jobType = UiRoutingSupport.formAttr(ctx, "jobType");
            String jobName = UiRoutingSupport.formAttr(ctx, "jobName");
            MultivaluedMap<String, String> allFormParams = UiRoutingSupport.allFormParams(ctx);

            if (jobType == null || jobType.isBlank()) {
                LOG.warnf("Job type is empty");
                UiRoutingSupport.sendFormError(ctx, "Job type is required");
                return;
            }

            Map<String, String> paramMap = extractParameterMap(allFormParams);

            createTemplateUseCase.execute(jobType, jobName, paramMap);

            UiRoutingSupport.sendModalClose(ctx, getDefaultTemplatesTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error creating template");
            UiRoutingSupport.sendFormError(ctx, "Error creating template: " + e.getMessage());
        }
    }

    public void handleUpdate(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        try {
            String jobType = UiRoutingSupport.formAttr(ctx, "jobType");
            String jobName = UiRoutingSupport.formAttr(ctx, "jobName");
            MultivaluedMap<String, String> allFormParams = UiRoutingSupport.allFormParams(ctx);

            LOG.debugf("Updating template %s - jobType=%s, jobName=%s", jobId, jobType, jobName);
            if (jobType == null || jobType.isBlank()) {
                LOG.warnf("Job type is empty");
                UiRoutingSupport.sendFormError(ctx, "Job type is required");
                return;
            }
            Map<String, String> paramMap = extractParameterMap(allFormParams);
            updateTemplateUseCase.execute(jobId, jobType, jobName, paramMap);
            UiRoutingSupport.sendModalClose(ctx, getDefaultTemplatesTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error updating template %s", jobId);
            UiRoutingSupport.sendFormError(ctx, "Error updating template: " + e.getMessage());
        }
    }

    public void handleDelete(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        deleteTemplateUseCase.execute(jobId);
        UiRoutingSupport.renderHtml(ctx, getDefaultTemplatesTable());
    }

    public void handleClone(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        cloneTemplateUseCase.execute(jobId, null);
        UiRoutingSupport.renderHtml(ctx, getDefaultTemplatesTable());
    }

    public void handleStart(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "admin")) {
            return;
        }
        UUID templateId = UiRoutingSupport.pathUuid(ctx, "id");
        startJobUseCase.execute(templateId, null, null, false);
        UiRoutingSupport.renderHtml(ctx, getDefaultTemplatesTable());
    }

    private TemplateInstance buildTemplatesTable(String search, String jobType,
                                                 int page, int size, String sortBy, String sortOrder) {
        List<ScheduledJobInfo> jobs = getTemplatesUseCase.execute();

        PaginationHelper.PaginationResult<ScheduledJobInfo> paginationResult =
                filterSortAndPaginate(jobs, jobType, search, sortBy, sortOrder, page, size, this::getComparator);

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
                sortOrder,
                uiConfig.showJobUuid()
        );
    }

    private Comparator<ScheduledJobInfo> getComparator(String sortBy) {
        return getComparator(sortBy, Comparator.comparing(ScheduledJobInfo::getJobName, String.CASE_INSENSITIVE_ORDER));
    }

    private TemplateInstance getDefaultTemplatesTable() {
        return buildTemplatesTable(null, "all", 0, 10, "jobName", "asc");
    }
}
