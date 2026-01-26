package ch.css.jobrunr.control.infrastructure.dev;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Development and Test Mode Role Augmentor.
 * <p>
 * This bean automatically grants configured roles to anonymous users during development and testing.
 * It is only active in 'dev' and 'test' build profiles and is excluded from production builds.
 * </p>
 * <p>
 * The roles to grant are configurable via the 'dev.test.roles' property, defaulting to
 * 'viewer,configurator,admin'. This allows easy testing of role-based UI features without
 * authentication setup.
 * </p>
 * <p>
 * <strong>Security Note:</strong> This bean is build-time conditional and will not be present
 * in production applications, ensuring no security bypass in production.
 * </p>
 */
@ApplicationScoped
@IfBuildProfile(anyOf = {"dev", "test"})
public class DevModeRoleAugmentor implements SecurityIdentityAugmentor {

    @ConfigProperty(name = "dev.test.roles", defaultValue = "viewer,configurator,admin")
    String testRoles;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
        if (identity.isAnonymous()) {
            builder.setAnonymous(false);
            builder.setPrincipal(new QuarkusPrincipal("dev-admin"));
        }
        for (String role : testRoles.split(",")) {
            builder.addRole(role.trim());
        }
        return Uni.createFrom().item(builder.build());
    }
}