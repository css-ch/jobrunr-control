package ch.css.jobrunr.control.adapter.ui.shell;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.qute.TemplateData;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds request-specific shell data, including role-filtered navigation.
 */
@ApplicationScoped
@TemplateData
public class JobRunrControlUiShell {

    private final Instance<JobRunrControlNavigationContributor> contributors;
    private final JobRunrControlUiBrandingProvider brandingProvider;
    private final DefaultJobRunrControlUiBrandingProvider defaultBrandingProvider;
    private final SecurityIdentity securityIdentity;

    public JobRunrControlUiShell(
            Instance<JobRunrControlNavigationContributor> contributors,
            JobRunrControlUiBrandingProvider brandingProvider,
            DefaultJobRunrControlUiBrandingProvider defaultBrandingProvider,
            SecurityIdentity securityIdentity) {
        this.contributors = contributors;
        this.brandingProvider = brandingProvider;
        this.defaultBrandingProvider = defaultBrandingProvider;
        this.securityIdentity = securityIdentity;
    }

    public JobRunrControlUiBranding branding() {
        return Optional.ofNullable(brandingProvider)
                .map(JobRunrControlUiBrandingProvider::branding)
                .flatMap(value -> value)
                .orElseGet(() -> defaultBrandingProvider.branding()
                        .orElseThrow(() -> new IllegalStateException("Default UI branding is not available")));
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
