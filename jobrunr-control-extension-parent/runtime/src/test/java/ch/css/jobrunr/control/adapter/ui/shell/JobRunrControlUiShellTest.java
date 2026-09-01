package ch.css.jobrunr.control.adapter.ui.shell;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobRunrControlUiShellTest {

    @Test
    void shouldSortNavigationContributorsByOrder() {
        JobRunrControlNavigationContributor later = contributor(200, "later");
        JobRunrControlNavigationContributor earlier = contributor(100, "earlier");
        Instance<JobRunrControlNavigationContributor> contributors = mock(Instance.class);
        JobRunrControlUiBrandingProvider brandingProvider = mock(JobRunrControlUiBrandingProvider.class);
        DefaultJobRunrControlUiBrandingProvider defaultBrandingProvider =
                mock(DefaultJobRunrControlUiBrandingProvider.class);
        when(contributors.stream()).thenReturn(Stream.of(later, earlier));
        when(brandingProvider.branding()).thenReturn(Optional.empty());
        when(defaultBrandingProvider.branding()).thenReturn(Optional.of(
                new JobRunrControlUiBranding("Default", "/", "clock", Optional.empty())));

        JobRunrControlUiShell shell = new JobRunrControlUiShell(
                contributors, brandingProvider, defaultBrandingProvider, mock(SecurityIdentity.class));

        assertThat(shell.navigationItems())
                .extracting(JobRunrControlNavigationItem::id)
                .containsExactly("earlier", "later");
        assertThat(shell.branding().applicationName()).isEqualTo("Default");
    }

    private JobRunrControlNavigationContributor contributor(int order, String id) {
        return new JobRunrControlNavigationContributor() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public List<JobRunrControlNavigationItem> navigationItems() {
                return List.of(new JobRunrControlNavigationItem(id, id, "/" + id, "icon", Set.of()));
            }
        };
    }
}
