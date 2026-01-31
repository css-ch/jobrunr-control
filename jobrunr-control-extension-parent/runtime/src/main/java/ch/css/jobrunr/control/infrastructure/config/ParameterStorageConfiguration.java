package ch.css.jobrunr.control.infrastructure.config;

import ch.css.jobrunr.control.domain.ParameterStorageStrategy;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for parameter storage.
 */
@ConfigMapping(prefix = "jobrunr.control.parameter-storage")
public interface ParameterStorageConfiguration {

    /**
     * Strategy for storing job parameters.
     * Default: INLINE
     */
    @WithDefault("INLINE")
    ParameterStorageStrategy strategy();

    /**
     * Persistence unit name for external parameter storage.
     * Default: &lt;default&gt; (Hibernate ORM default persistence unit)
     * <p>
     * Note: This is a build-time configuration that must be set before building the application.
     */
    @WithDefault("<default>")
    String persistenceUnitName();

    /**
     * Cleanup configuration for external parameter storage.
     */
    CleanupConfig cleanup();

    interface CleanupConfig {
        /**
         * Enable automatic cleanup of orphaned parameter sets.
         * Default: true
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Retention period in days for parameter sets.
         * Parameter sets older than this will be deleted if not accessed.
         * Default: 30
         */
        @WithDefault("30")
        int retentionDays();
    }
}
