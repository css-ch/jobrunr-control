package ch.css.jobrunr.control.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application root. Declaring {@code @ApplicationPath("/api")} prefixes every JAX-RS
 * resource with {@code /api} — the JobRunr Control REST API is therefore reachable at
 * {@code /api/q/jobrunr-control/api/*}. The UI dashboard is unaffected because it is mounted
 * under Quarkus' non-application root path (see issue #25).
 */
@ApplicationPath("/api")
public class ExampleApplication extends Application {
}
