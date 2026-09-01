package ch.css.jobrunr.control.example.ui;

import ch.css.jobrunr.control.adapter.ui.shell.JobRunrControlNavigationContributor;
import ch.css.jobrunr.control.adapter.ui.shell.JobRunrControlNavigationItem;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;

/**
 * Example host-application navigation enabled with the {@code ui-extensions} profile.
 */
@ApplicationScoped
@IfBuildProfile("ui-extensions")
public class ExampleNavigationContributor implements JobRunrControlNavigationContributor {

    @Override
    public int order() {
        return 500;
    }

    @Override
    public List<JobRunrControlNavigationItem> navigationItems() {
        return List.of(new JobRunrControlNavigationItem(
                "google-search",
                "Google-Suche",
                "https://www.google.com/search?q=JobRunr+Pro",
                "search",
                Set.of()));
    }
}
