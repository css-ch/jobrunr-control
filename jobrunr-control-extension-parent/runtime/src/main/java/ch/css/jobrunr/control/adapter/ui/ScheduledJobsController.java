package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.discovery.GetJobParametersUseCase;
import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.application.scheduling.*;
import ch.css.jobrunr.control.domain.*;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
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
@ApplicationScoped
@SuppressWarnings("java:S1192") // "external" filter literal duplication is acceptable for query param clarity
public class ScheduledJobsController extends BaseController {

    private static final Logger LOG = Logger.getLogger(ScheduledJobsController.class);

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {

        private Templates() {
            // Qute class
        }

        public static native TemplateInstance scheduledJobs(List<JobDefinition> availableJobTypes);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {

        private Components() {
            // Qute class
        }

        @SuppressWarnings("java:S107")
        public static native TemplateInstance scheduledJobsTable(List<ScheduledJobInfoView> jobs,
                                                                 PaginationHelper.PaginationMetadata pagination,
                                                                 List<TemplateExtensions.PageItem> pageRange,
                                                                 String search, String filter, String jobType,
                                                                 String sortBy, String sortOrder,
                                                                 boolean showUuid);

        public static native TemplateInstance paramInputs(List<JobParameter> parameters,
                                                          List<JobParameterSection> parameterSections,
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
                                                      List<JobParameter> parameters,
                                                      List<JobParameterSection> parameterSections);
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
    private final JobRunrControlUiConfig uiConfig;

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
            ExecuteScheduledJobUseCase executeScheduledJobUseCase,
            JobRunrControlUiConfig uiConfig,
            JobSearchUtils searchUtils) {
        super(searchUtils);
        this.discoverJobsUseCase = discoverJobsUseCase;
        this.getJobParametersUseCase = getJobParametersUseCase;
        this.resolveParametersUseCase = resolveParametersUseCase;
        this.getScheduledJobsUseCase = getScheduledJobsUseCase;
        this.getScheduledJobByIdUseCase = getScheduledJobByIdUseCase;
        this.createJobUseCase = createJobUseCase;
        this.updateJobUseCase = updateJobUseCase;
        this.deleteJobUseCase = deleteJobUseCase;
        this.executeScheduledJobUseCase = executeScheduledJobUseCase;
        this.uiConfig = uiConfig;
    }

    public void handleIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        List<JobDefinition> availableJobTypes = getAvailableJobDefinitions(discoverJobsUseCase);
        UiRoutingSupport.renderHtml(ctx, Templates.scheduledJobs(availableJobTypes));
    }

    public void handleTable(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildScheduledJobsTable(
                UiRoutingSupport.queryParam(ctx, "search"),
                UiRoutingSupport.queryParam(ctx, "filter", "all"),
                UiRoutingSupport.queryParam(ctx, "jobType"),
                UiRoutingSupport.intQueryParam(ctx, "page", 0),
                UiRoutingSupport.intQueryParam(ctx, "size", 10),
                UiRoutingSupport.queryParam(ctx, "sortBy", "scheduledAt"),
                UiRoutingSupport.queryParam(ctx, "sortOrder", "asc")));
    }

    public void handleNewModal(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx,
                Modals.jobForm(getSortedJobDefinitions(discoverJobsUseCase), false, null, null, null));
    }

    public void handleEditModal(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        List<JobDefinition> jobDefinitions = getSortedJobDefinitions(discoverJobsUseCase);

        ScheduledJobInfo jobInfo = getScheduledJobByIdUseCase.execute(jobId)
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

        UiRoutingSupport.renderHtml(ctx, Modals.jobForm(jobDefinitions, true,
                resolvedData.jobInfoWithResolvedParams, resolvedData.parameters, resolvedData.parameterSections));
    }

    public void handleParametersModal(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobType = UiRoutingSupport.queryParam(ctx, "jobType");
        LOG.debugf("handleParametersModal jobType='%s'", jobType);

        if (jobType == null || jobType.isBlank()) {
            LOG.warnf("jobType is empty");
            UiRoutingSupport.renderHtml(ctx, Components.paramInputs(List.of(), List.of(), null));
            return;
        }

        try {
            GetJobParametersUseCase.Result result = getJobParametersUseCase.execute(jobType);
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Found %s parameters for job type '%s'", result.parameters().size(), jobType);
                for (JobParameter param : result.parameters()) {
                    LOG.debugf("  - Parameter: %s (type: %s, required: %s, defaultValue: '%s')",
                            param.name(), param.type(), param.required(), param.defaultValue());
                }
            }
            UiRoutingSupport.renderHtml(ctx,
                    Components.paramInputs(result.parameters(), result.parameterSections(), null));
        } catch (Exception e) {
            LOG.errorf(e, "Error getting parameters for job type '%s'", jobType);
            UiRoutingSupport.renderHtml(ctx, Components.paramInputs(List.of(), List.of(), null));
        }
    }

    public void handleCreate(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        try {
            String jobType = UiRoutingSupport.formAttr(ctx, "jobType");
            String jobName = UiRoutingSupport.formAttr(ctx, "jobName");
            String triggerType = UiRoutingSupport.formAttr(ctx, "triggerType");
            String scheduledAt = UiRoutingSupport.formAttr(ctx, "scheduledAt");
            MultivaluedMap<String, String> allFormParams = UiRoutingSupport.allFormParams(ctx);

            if (jobType == null || jobType.isBlank()) {
                LOG.warnf("Job type is empty");
                UiRoutingSupport.sendFormError(ctx, "Job type is required");
                return;
            }

            Map<String, String> paramMap = extractParameterMap(allFormParams);
            boolean isExternalTrigger = "external".equals(triggerType);
            Instant scheduledTime = isExternalTrigger ? null : parseScheduledTime(scheduledAt);

            createJobUseCase.execute(jobType, jobName, paramMap, scheduledTime, isExternalTrigger);

            UiRoutingSupport.sendModalClose(ctx, getDefaultScheduledJobsTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error creating job");
            UiRoutingSupport.sendFormError(ctx, "Error creating job: " + e.getMessage());
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
            String triggerType = UiRoutingSupport.formAttr(ctx, "triggerType");
            String scheduledAt = UiRoutingSupport.formAttr(ctx, "scheduledAt");
            MultivaluedMap<String, String> allFormParams = UiRoutingSupport.allFormParams(ctx);

            LOG.infof("Updating job %s - jobType=%s, jobName=%s, triggerType=%s, scheduledAt=%s",
                    jobId, jobType, jobName, triggerType, scheduledAt);
            LOG.debugf("All form parameters: %s", allFormParams);

            if (jobType == null || jobType.isBlank()) {
                LOG.warnf("Job type is empty");
                UiRoutingSupport.sendFormError(ctx, "Job type is required");
                return;
            }

            boolean isExternalTrigger = "external".equals(triggerType);
            Instant scheduledTime = isExternalTrigger ? null : parseScheduledTime(scheduledAt);

            Map<String, String> paramMap = extractParameterMap(allFormParams);
            updateJobUseCase.execute(jobId, jobType, jobName, paramMap, scheduledTime, isExternalTrigger);

            UiRoutingSupport.sendModalClose(ctx, getDefaultScheduledJobsTable());
        } catch (Exception e) {
            LOG.errorf(e, "Error updating job %s", jobId);
            UiRoutingSupport.sendFormError(ctx, "Error updating job: " + e.getMessage());
        }
    }

    public void handleDelete(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "configurator", "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        deleteJobUseCase.execute(jobId);
        UiRoutingSupport.renderHtml(ctx, getDefaultScheduledJobsTable());
    }

    public void handleExecute(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "admin")) {
            return;
        }
        UUID jobId = UiRoutingSupport.pathUuid(ctx, "id");
        executeScheduledJobUseCase.execute(jobId);
        UiRoutingSupport.renderHtml(ctx, getDefaultScheduledJobsTable());
    }

    @SuppressWarnings("java:S107")
    private TemplateInstance buildScheduledJobsTable(String search, String filter, String jobType,
                                                     int page, int size, String sortBy, String sortOrder) {
        LOG.infof("buildScheduledJobsTable page=%d, size=%d, sortBy=%s, sortOrder=%s, search=%s, filter=%s, jobType=%s",
                page, size, sortBy, sortOrder, search, filter, jobType);

        List<ScheduledJobInfo> jobs = getScheduledJobsUseCase.execute();

        jobs = jobs.stream()
                .filter(job -> !job.isTemplate())
                .toList();

        if ("external".equals(filter)) {
            jobs = jobs.stream()
                    .filter(ScheduledJobInfo::isExternallyTriggerable)
                    .toList();
        } else if ("scheduled".equals(filter)) {
            jobs = jobs.stream()
                    .filter(j -> !j.isExternallyTriggerable())
                    .toList();
        }

        PaginationHelper.PaginationResult<ScheduledJobInfo> paginationResult =
                filterSortAndPaginate(jobs, jobType, search, sortBy, sortOrder, page, size, this::getComparator);

        List<ScheduledJobInfoView> jobViews = paginationResult.pageItems().stream()
                .map(job -> toView(job, resolveParametersUseCase))
                .toList();

        LOG.infof("Returning %d job views to template (expected max: %d)", jobViews.size(), size);

        return Components.scheduledJobsTable(
                jobViews,
                paginationResult.metadata(),
                paginationResult.pageRange(),
                search != null ? search : "",
                filter,
                jobType != null ? jobType : "all",
                sortBy,
                sortOrder,
                uiConfig.showJobUuid()
        );
    }

    private Comparator<ScheduledJobInfo> getComparator(String sortBy) {
        return getComparator(sortBy, Comparator.comparing(ScheduledJobInfo::getScheduledAt));
    }

    private TemplateInstance getDefaultScheduledJobsTable() {
        return buildScheduledJobsTable(null, "all", null, 0, 10, "scheduledAt", "asc");
    }
}
