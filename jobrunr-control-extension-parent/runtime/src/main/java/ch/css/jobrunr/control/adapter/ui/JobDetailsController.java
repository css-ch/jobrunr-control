package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.details.GetJobDetailsParametersUseCase;
import ch.css.jobrunr.control.application.details.GetJobDetailsRecapUseCase;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterSection;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main Dashboard Controller.
 * Root handler delegates to the scheduled-jobs template.
 */
@ApplicationScoped
public class JobDetailsController {

    private final GetJobDetailsParametersUseCase getJobDetailsParametersUseCase;
    private final GetJobDetailsRecapUseCase getJobDetailsRecapUseCase;

    @Inject
    public JobDetailsController(GetJobDetailsParametersUseCase getJobDetailsParametersUseCase, GetJobDetailsRecapUseCase getJobDetailsRecapUseCase) {
        this.getJobDetailsParametersUseCase = getJobDetailsParametersUseCase;
        this.getJobDetailsRecapUseCase = getJobDetailsRecapUseCase;
    }

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        private Templates() {
            // Utility class
        }

        public static native TemplateInstance jobDetails(String jobId, String jobType, String title, String subtitle, String postTitle);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        private Components() {
            // Utility class
        }

        public static native TemplateInstance jobDetailsRecap(GetJobDetailsRecapUseCase.Result recap);
        public static native TemplateInstance jobDetailsParameter(Map<String, Object> parameters, List<JobParameterSection> parameterSections, List<JobParameter> parameterDefinitions, boolean showEmptyParameters);
        public static native TemplateInstance jobDetailsMessages(String jobId, String jobType, String search);
    }

    public void handleIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobId = UiRoutingSupport.queryParam(ctx, "jobId");
        String jobType = UiRoutingSupport.queryParam(ctx, "jobType");
        String jobName = UiRoutingSupport.queryParam(ctx, "jobName");
        
        // Construct title and subtitle on Java side (no template interpolation needed)
        String title = "Batch Detail " + jobName;
        UiRoutingSupport.renderHtml(ctx, JobDetailsController.Templates.jobDetails(jobId, jobType, title, jobId, jobType));
    }

    public void handleDetailsRecap(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildRecapTable(
                UiRoutingSupport.queryParam(ctx, "jobId")));
    }

    public void handleDetailsParameter(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildParameterTable(
                UiRoutingSupport.queryParam(ctx, "jobId")
        ));
    }

    public void handleDetailsMessages(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildMessagesTable(
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType"),
                UiRoutingSupport.queryParam(ctx, "search")));
    }

    private TemplateInstance buildRecapTable(String jobId) {
        GetJobDetailsRecapUseCase.Result recapData = getJobDetailsRecapUseCase.execute(jobIdAsUUID(jobId));
        return JobDetailsController.Components.jobDetailsRecap(recapData);
    }

    private TemplateInstance buildParameterTable(String jobId) {
        try {
            GetJobDetailsParametersUseCase.Result result = getJobDetailsParametersUseCase.execute(jobId);
            return JobDetailsController.Components.jobDetailsParameter(result.parameters(), result.parameterSections(), result.parameterDefinitions(), result.showEmptyParameters());
        } catch (Exception e) {
            // Return with empty parameters if loading fails
            return JobDetailsController.Components.jobDetailsParameter(Map.of(), List.of(), List.of(), false);
        }
    }

    private TemplateInstance buildMessagesTable(String jobId, String jobType, String search) {
        return JobDetailsController.Components.jobDetailsMessages(jobId, jobType, search);
    }

    private UUID jobIdAsUUID(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Job ID must not be null or empty");
        }
        try {
            return UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        }

    }
}


