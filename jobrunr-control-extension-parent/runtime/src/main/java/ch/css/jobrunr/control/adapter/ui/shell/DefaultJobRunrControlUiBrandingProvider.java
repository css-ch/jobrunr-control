package ch.css.jobrunr.control.adapter.ui.shell;

import ch.css.jobrunr.control.adapter.ui.DashboardPaths;
import ch.css.jobrunr.control.adapter.ui.JobRunrControlUiConfig;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Default branding used when JobRunr Control is running standalone.
 */
@DefaultBean
@ApplicationScoped
public class DefaultJobRunrControlUiBrandingProvider implements JobRunrControlUiBrandingProvider {

    private final DashboardPaths dashboardPaths;
    private final JobRunrControlUiConfig uiConfig;

    public DefaultJobRunrControlUiBrandingProvider(
            DashboardPaths dashboardPaths,
            JobRunrControlUiConfig uiConfig) {
        this.dashboardPaths = dashboardPaths;
        this.uiConfig = uiConfig;
    }

    @Override
    public Optional<JobRunrControlUiBranding> branding() {
        JobRunrControlUiStage stage = uiConfig.stage()
                .filter(value -> !value.isBlank())
                .map(value -> new JobRunrControlUiStage(value.trim(), "", ""))
                .orElse(null);
        return Optional.of(new JobRunrControlUiBranding(
                "JobRunr Control",
                dashboardPaths.basePath() + "/history",
                "clock-history",
                Optional.ofNullable(stage)));
    }
}
