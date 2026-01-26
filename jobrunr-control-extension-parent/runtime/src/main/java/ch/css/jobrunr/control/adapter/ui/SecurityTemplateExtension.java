package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.arc.Arc;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.security.identity.SecurityIdentity;

@TemplateExtension(namespace = "security")
public class SecurityTemplateExtension {
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

}