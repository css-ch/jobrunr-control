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
 * Security augmentor that grants the {@code admin} role when OIDC is disabled.
 * <p>
 * When {@code quarkus.oidc.enabled=false}, this augmentor automatically grants
 * the {@code admin} role to all requests, satisfying every {@code @RolesAllowed}
 * check in the extension and effectively bypassing authentication.
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

    private final AtomicBoolean warningLogged = new AtomicBoolean(false);

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
    boolean oidcEnabled;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        // If OIDC is enabled, do NOT modify the identity - use normal OIDC authentication
        if (oidcEnabled) {
            return Uni.createFrom().item(identity);
        }

        // OIDC is disabled: grant admin role to satisfy all @RolesAllowed checks
        if (warningLogged.compareAndSet(false, true)) {
            LOG.warnf("OIDC disabled: Granting admin role to all requests. Ensure this is intentional!");
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        // If anonymous, create a synthetic principal
        if (identity.isAnonymous()) {
            builder.setAnonymous(false);
            builder.setPrincipal(new QuarkusPrincipal("anonymous-user"));
        }

        builder.addRole("admin");

        return Uni.createFrom().item(builder.build());
    }
}


