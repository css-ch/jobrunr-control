package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.dashboard.GetDashboardParametersUseCase;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Main Dashboard Controller.
 * Root handler delegates to the scheduled-jobs template.
 */
@ApplicationScoped
public class JobDetailsController {

    private final GetDashboardParametersUseCase getDashboardParametersUseCase;

    @Inject
    public JobDetailsController(GetDashboardParametersUseCase getDashboardParametersUseCase) {
        this.getDashboardParametersUseCase = getDashboardParametersUseCase;
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

        public static native TemplateInstance jobDetailsRecap(String jobId, String jobType);
        public static native TemplateInstance jobDetailsParameter(String jobId, String jobType, Map<String, Object> parameters);
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
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType")));
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
        return JobDetailsController.Components.jobDetailsRecap(jobId, "");
    }

    private TemplateInstance buildParameterTable(String jobId, String jobType) {
        try {
            GetDashboardParametersUseCase.Result result = getDashboardParametersUseCase.execute(jobId);
            return JobDetailsController.Components.jobDetailsParameter(jobId, jobType, result.parameters());
        } catch (Exception e) {
            // Return with empty parameters if loading fails
            return JobDetailsController.Components.jobDetailsParameter(jobId, jobType, Map.of());
        }
    }

    private TemplateInstance buildMessagesTable(String jobId, String jobType, String search) {
        return JobDetailsController.Components.jobDetailsMessages(jobId, jobType, search);
    }
}


