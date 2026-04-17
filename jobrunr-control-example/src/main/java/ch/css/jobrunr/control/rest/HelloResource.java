package ch.css.jobrunr.control.rest;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Minimal JAX-RS resource used to exercise the consumer-owned
 * {@link jakarta.ws.rs.ApplicationPath} configuration.
 * <p>
 * With {@link ExampleApplication} declaring {@code @ApplicationPath("/api")}, this endpoint
 * is served at {@code /api/hello}.
 */
@Path("/hello")
public class HelloResource {

    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from JobRunr Control example";
    }
}
