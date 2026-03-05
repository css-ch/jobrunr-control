package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration for the JobRunr Control UI.
 */
@ConfigMapping(prefix = "quarkus.jobrunr-control.ui")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JobRunrControlUiConfig {

    /**
     * Whether to display the job UUID column in all job tables.
     * Default: false
     */
    @WithDefault("false")
    boolean showJobUuid();
}
