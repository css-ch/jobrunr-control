package ch.css.jobrunr.control.adapter.ui.shell;

import ch.css.jobrunr.control.adapter.ui.DashboardPaths;
import ch.css.jobrunr.control.adapter.ui.JobRunrControlUiConfig;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Default navigation entries for standalone JobRunr Control.
 */
@ApplicationScoped
public class DefaultJobRunrControlNavigationContributor implements JobRunrControlNavigationContributor {

    private final DashboardPaths dashboardPaths;
    private final JobRunrControlUiConfig uiConfig;

    public DefaultJobRunrControlNavigationContributor(
            DashboardPaths dashboardPaths,
            JobRunrControlUiConfig uiConfig) {
        this.dashboardPaths = dashboardPaths;
        this.uiConfig = uiConfig;
    }

    @Override
    public List<JobRunrControlNavigationItem> navigationItems() {
        String basePath = dashboardPaths.basePath();
        Set<String> readRoles = Set.of("viewer", "configurator", "admin");
        return Stream.of(
                        new JobRunrControlNavigationItem(
                                "templates", "Templates", basePath + "/templates", "file-earmark-text", readRoles),
                        new JobRunrControlNavigationItem(
                                "scheduled", "Geplante Jobs", basePath + "/scheduled", "calendar-check", readRoles),
                        new JobRunrControlNavigationItem(
                                "history", "Historie", basePath + "/history", "clock-history", readRoles)
                )
                .filter(item -> !"scheduled".equals(item.id()) || uiConfig.showScheduledJobs())
                .toList();
    }
}
