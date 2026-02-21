package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.arc.Arc;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@TemplateExtension(namespace = "security")
public class SecurityTemplateExtension {

    private SecurityTemplateExtension() {
        // Utility class - prevent instantiation
    }

    @SuppressWarnings("unused") // Used by Qute templates
    public static boolean hasOneOfRole(String... roles) {
        SecurityIdentity identity = Arc.container().instance(SecurityIdentity.class).get();
        if (identity == null) return false;
        for (String role : roles) {
            if (identity.hasRole(role.trim())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused") // Used by Qute templates
    public static String currentUser() {
        SecurityIdentity identity = Arc.container().instance(SecurityIdentity.class).get();
        if (identity == null || identity.isAnonymous()) return "";
        return identity.getPrincipal().getName();
    }

    /**
     * Checks if OIDC authentication is enabled.
     * Used in templates to conditionally show/hide login/logout UI elements.
     *
     * @return true if OIDC is enabled (default), false otherwise
     */
    @SuppressWarnings("unused") // Used by Qute templates
    public static boolean isOidcEnabled() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("quarkus.oidc.enabled", Boolean.class)
                .orElse(true); // Default: OIDC enabled
    }

}