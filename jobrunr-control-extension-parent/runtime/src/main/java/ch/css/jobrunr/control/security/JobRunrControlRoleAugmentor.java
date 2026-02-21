package ch.css.jobrunr.control.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Security augmentor that grants all roles when OIDC is disabled.
 * <p>
 * When {@code quarkus.oidc.enabled=false}, this augmentor automatically grants
 * all required roles (viewer, configurator, admin, api-reader, api-executor)
 * to all requests, effectively bypassing authentication.
 * </p>
 * <p>
 * <strong>Security Warning:</strong> This is intended for development and testing
 * environments only. In production, always use OIDC authentication by setting
 * {@code quarkus.oidc.enabled=true} (default).
 * </p>
 * <p>
 * <strong>Architecture Note:</strong> This class belongs to the Infrastructure/Security
 * layer in the Hexagonal Architecture. It provides a security adapter that can be
 * enabled/disabled based on deployment requirements.
 * </p>
 */
@ApplicationScoped
public class JobRunrControlRoleAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = Logger.getLogger(JobRunrControlRoleAugmentor.class);

    private static final String[] ALL_ROLES = {
            "viewer", "configurator", "admin", "api-reader", "api-executor"
    };

    private final AtomicBoolean warningLogged = new AtomicBoolean(false);

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
    boolean oidcEnabled;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        // If OIDC is enabled, do NOT modify the identity - use normal OIDC authentication
        if (oidcEnabled) {
            return Uni.createFrom().item(identity);
        }

        // OIDC is disabled: grant all roles to enable development/testing without authentication
        if (warningLogged.compareAndSet(false, true)) {
            LOG.warnf("OIDC disabled: Granting all roles to requests. This should ONLY be used in development/testing!");
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        // If anonymous, create a test principal
        if (identity.isAnonymous()) {
            builder.setAnonymous(false);
            builder.setPrincipal(new QuarkusPrincipal("anonymous-user"));
        }

        // Grant all roles
        for (String role : ALL_ROLES) {
            builder.addRole(role);
        }

        return Uni.createFrom().item(builder.build());
    }
}


