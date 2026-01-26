package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Main Dashboard Controller.
 * Root path redirects to scheduled jobs using type-safe Qute templates.
 */
@Path("/q/jobrunr-control")
public class DashboardController {

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        // Redirect to scheduler overview - use the template from ScheduledJobsController
        return ScheduledJobsController.Templates.scheduledJobs();
    }
}
