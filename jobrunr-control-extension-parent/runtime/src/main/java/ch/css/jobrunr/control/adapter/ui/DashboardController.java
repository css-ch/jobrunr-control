package ch.css.jobrunr.control.adapter.ui;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Main Dashboard Controller.
 * Root handler delegates to the scheduled-jobs template.
 */
@ApplicationScoped
public class DashboardController {

    public void handleIndex(RoutingContext ctx) {
        if (!UiRoutingSupport.requireAnyRole(ctx, "viewer", "configurator", "admin")) {
            return;
        }
        UiRoutingSupport.renderHtml(ctx, ScheduledJobsController.Templates.scheduledJobs(List.of()));
    }
}
