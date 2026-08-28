package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Runtime configuration for the JobRunr Control UI.
 */
@ConfigMapping(prefix = "quarkus.jobrunr-control.ui")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JobRunrControlUiConfig {

    /**
     * Whether to display the scheduled jobs menu entry.
     */
    @WithDefault("true")
    boolean showScheduledJobs();

    /**
     * Whether to display the job UUID column in all job tables.
     * Default: false
     * Attribute: quarkus.jobrunr-control.ui.show-job-uuid
     */
    @WithDefault("false")
    boolean showJobUuid();

    /**
     * Whether to display the business Status in den Execution-History Table.
     * The business Status must be set by the application.
     * Attribute: quarkus.jobrunr-control.ui.show-business-status
     */
    @WithDefault("false")
    boolean showBusinessStatus();

    /**
     * Deployment stage displayed by the default UI branding.
     * Any non-blank value is displayed as a stage badge.
     */
    Optional<String> stage();
}
