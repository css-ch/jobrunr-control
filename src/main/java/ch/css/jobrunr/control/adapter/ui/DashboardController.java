package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Main Dashboard Controller.
 * Root path redirects to scheduled jobs.
 */
@Path("/")
public class DashboardController {

    @Inject
    @io.quarkus.qute.Location("scheduled-jobs.html")
    Template scheduledJobs;

    @GET
    @RolesAllowed({"viewer", "configurator", "admin"})
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        // Redirect to scheduler overview
        return scheduledJobs.instance();
    }
}
