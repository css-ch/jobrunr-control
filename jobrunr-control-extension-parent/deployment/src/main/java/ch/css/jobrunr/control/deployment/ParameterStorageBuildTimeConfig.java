package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.domain.ParameterStorageStrategy;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration for parameter storage.
 */
@ConfigMapping(prefix = "quarkus.jobrunr-control.parameter-storage")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ParameterStorageBuildTimeConfig {

    /**
     * Parameter storage strategy (INLINE or EXTERNAL).
     * Default: INLINE
     */
    @WithDefault("INLINE")
    ParameterStorageStrategy strategy();
}
