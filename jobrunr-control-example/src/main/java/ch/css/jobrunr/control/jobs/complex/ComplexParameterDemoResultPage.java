package ch.css.jobrunr.control.jobs.complex;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/mybatch/result")
@ApplicationScoped
@RolesAllowed({"viewer", "configurator", "admin"})
public class ComplexParameterDemoResultPage {
    
    @Inject
    Engine engine;
    
    private static final String RESULT_TEMPLATE = """
        <html>
        <head><title>Job Result</title></head>
        <body>
            <h1>Job Result for Job ID: {jobId}</h1>
            <p>Stage: {stage}</p>
            <p>Start Date: {startDate}</p>
            <p>End Date: {endDate}</p>
        </body>
        </html>
    """;

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getResultPage(@PathParam("id") String jobId,
                                          @QueryParam("stage") String stage,
                                          @QueryParam("startDate") String startDate,
                                          @QueryParam("endDate") String endDate
                                          ) {
        Template resultTemplate = engine.parse(RESULT_TEMPLATE);
        
        return resultTemplate.data("jobId", jobId, "stage", stage, "startDate", startDate, "endDate", endDate);
    }

}
