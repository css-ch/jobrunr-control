package ch.css.jobrunr.control.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration for parameter storage.
 */
@ConfigMapping(prefix = "jobrunr.control.parameter-storage")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ParameterStorageBuildTimeConfig {

    /**
     * Persistence unit name for external parameter storage.
     * Default: &lt;default&gt; (Hibernate ORM default persistence unit)
     */
    @WithDefault("<default>")
    String persistenceUnitName();
}
