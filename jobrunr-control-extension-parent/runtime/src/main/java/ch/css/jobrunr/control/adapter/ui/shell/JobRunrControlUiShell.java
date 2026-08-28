package ch.css.jobrunr.control.adapter.ui.shell;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.qute.TemplateData;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;

/**
 * Builds request-specific shell data, including role-filtered navigation.
 */
@ApplicationScoped
@TemplateData
public class JobRunrControlUiShell {

    private final Instance<JobRunrControlNavigationContributor> contributors;
    private final JobRunrControlUiBrandingProvider brandingProvider;
    private final SecurityIdentity securityIdentity;

    public JobRunrControlUiShell(
            Instance<JobRunrControlNavigationContributor> contributors,
            JobRunrControlUiBrandingProvider brandingProvider,
            SecurityIdentity securityIdentity) {
        this.contributors = contributors;
        this.brandingProvider = brandingProvider;
        this.securityIdentity = securityIdentity;
    }

    public JobRunrControlUiBranding branding() {
        return brandingProvider.branding().orElse(null);
    }

    public List<JobRunrControlNavigationItem> navigationItems() {
        return contributors.stream()
                .sorted(Comparator.comparingInt(JobRunrControlNavigationContributor::order)
                        .thenComparing(contributor -> contributor.getClass().getName()))
                .flatMap(contributor -> contributor.navigationItems().stream())
                .filter(item -> hasRequiredRole(item.requiredRoles()))
                .toList();
    }

    private boolean hasRequiredRole(Iterable<String> requiredRoles) {
        for (String role : requiredRoles) {
            if (securityIdentity != null && securityIdentity.hasRole(role)) {
                return true;
            }
        }
        return !requiredRoles.iterator().hasNext();
    }
}
