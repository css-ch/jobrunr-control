package ch.css.jobrunr.control.security;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.dashboard.server.security.JobRunrUser;
import org.jobrunr.dashboard.server.security.JobRunrUserAuthorizationRules;
import org.jobrunr.dashboard.server.security.JobRunrUserContext;
import org.jobrunr.quarkus.dashboard.authentication.JobRunQuarkusAuthenticationFilter;

/**
 * Initialises the {@link JobRunrUserContext} for every request to the embedded
 * JobRunr Pro Dashboard so that the dashboard does not show
 * "You do not have access to the JobRunr Pro Dashboard".
 *
 * <p>When {@code quarkus.jobrunr.dashboard.type=embedded}, JobRunr Pro delegates
 * authentication entirely to a CDI bean of type {@link JobRunQuarkusAuthenticationFilter}.
 * Without a bean, no {@link JobRunrUser} is ever set in the Vert.x request context, and
 * the dashboard frontend returns the "no access" message.
 *
 * <p>Access control is enforced exclusively by Quarkus HTTP Auth Policies
 * ({@code jobrunr-api-read} / {@code jobrunr-api-write}) before the request reaches
 * this filter. JobRunr Pro therefore never sees an unauthenticated or unauthorised
 * request, and granting {@link JobRunrUserAuthorizationRules#allowAll()} here is safe.
 *
 * <p>This bean is registered as an explicit {@code @ApplicationScoped} bean via
 * {@code AdditionalBeanBuildItem} in the deployment module. It overrides the default
 * {@code JobRunrQuarkusAnonymousAuthenticationFilter} produced by JobRunr Pro's
 * {@code @DefaultBean} mechanism: when a non-default bean of the same type exists,
 * ArC automatically suppresses the {@code @DefaultBean}.
 */
@ApplicationScoped
public class JobRunrDashboardUserContextFilter implements JobRunQuarkusAuthenticationFilter {

    private static final org.jboss.logging.Logger LOG =
            org.jboss.logging.Logger.getLogger(JobRunrDashboardUserContextFilter.class);

    /**
     * A single, stateless {@link JobRunrUser} with full {@code allowAll()} authorisation.
     * Safe because every request is already authenticated by Quarkus before reaching this filter.
     */
    private static final JobRunrUser ALLOW_ALL_USER =
            new JobRunrUser(null, null, JobRunrUserAuthorizationRules.allowAll());

    @Override
    public void filter(RoutingContext ctx) {
        LOG.debugf("JobRunrDashboardUserContextFilter invoked for: %s (hasCurrentUser=%b)",
                ctx.request().path(), JobRunrUserContext.hasCurrentUser());
        if (!JobRunrUserContext.hasCurrentUser()) {
            LOG.infof("Setting anonymous allow-all JobRunrUser for dashboard request: %s", ctx.request().path());
            JobRunrUserContext.setCurrentUser(ALLOW_ALL_USER);
        }
        ctx.next();
    }
}

