package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.dashboard.GetDashboardParametersUseCase;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * Main Dashboard Controller.
 * Root handler delegates to the scheduled-jobs template.
 */
@ApplicationScoped
public class DashboardController {

    private final GetDashboardParametersUseCase getDashboardParametersUseCase;

    @Inject
    public DashboardController(GetDashboardParametersUseCase getDashboardParametersUseCase) {
        this.getDashboardParametersUseCase = getDashboardParametersUseCase;
    }

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Templates {
        private Templates() {
            // Utility class
        }

        public static native TemplateInstance dashboard(String jobId, String jobType, String title, String subtitle, String postTitle);
    }

    @CheckedTemplate(basePath = "components", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    public static class Components {
        private Components() {
            // Utility class
        }

        public static native TemplateInstance dashboardRecap(String jobId, String jobType);
        public static native TemplateInstance dashboardParameter(String jobId, String jobType, Map<String, Object> parameters);
        public static native TemplateInstance dashboardMessages(String jobId, String jobType, String search);
    }

    public void handleIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, ScheduledJobsController.Templates.scheduledJobs(List.of()));
    }

    public void handleBatchIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        String jobId = UiRoutingSupport.queryParam(ctx, "jobId");
        String jobType = UiRoutingSupport.queryParam(ctx, "jobType");
        String jobName = UiRoutingSupport.queryParam(ctx, "jobName");
        
        // Construct title and subtitle on Java side (no template interpolation needed)
        String title = "Batch Detail " + jobName;
        String subtitle = jobId;
        
        // Construct postTitle element (e.g., small badge or button on the right)
        String postTitle = jobType;
        
        UiRoutingSupport.renderHtml(ctx, DashboardController.Templates.dashboard(jobId, jobType, title, subtitle, postTitle));
    }

    public void handleDashboardRecap(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildDashboardRecapTable(
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType")));
    }

    public void handleDashboardParameter(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildDashboardParameterTable(
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType")));
    }

    public void handleDashboardMessages(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, buildDashboardMessagesTable(
                UiRoutingSupport.queryParam(ctx, "jobId"),
                UiRoutingSupport.queryParam(ctx, "jobType"),
                UiRoutingSupport.queryParam(ctx, "search")));
    }

    private TemplateInstance buildDashboardRecapTable(String jobId, String jobType) {
        return DashboardController.Components.dashboardRecap(jobId, jobType);
    }

    private TemplateInstance buildDashboardParameterTable(String jobId, String jobType) {
        try {
            GetDashboardParametersUseCase.Result result = getDashboardParametersUseCase.execute(jobId);
            return DashboardController.Components.dashboardParameter(jobId, jobType, result.parameters());
        } catch (Exception e) {
            // Return with empty parameters if loading fails
            return DashboardController.Components.dashboardParameter(jobId, jobType, Map.of());
        }
    }

    private TemplateInstance buildDashboardMessagesTable(String jobId, String jobType, String search) {
        return DashboardController.Components.dashboardMessages(jobId, jobType, search);
    }
}


