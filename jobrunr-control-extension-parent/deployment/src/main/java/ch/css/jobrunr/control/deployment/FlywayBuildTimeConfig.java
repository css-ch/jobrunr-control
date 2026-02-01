package ch.css.jobrunr.control.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Build-time configuration for JobRunr Control Flyway migrations.
 */
@ConfigMapping(prefix = "jobrunr.control.flyway")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface FlywayBuildTimeConfig {

    /**
     * Enable or disable Flyway migrations for JobRunr Control.
     * When disabled, the Flyway extension dependency is not required.
     * Default: true
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Named datasource to use for migrations.
     * If not set, uses the default datasource.
     * This allows using a different connection with elevated privileges for migrations.
     */
    @WithDefault("<default>")
    String datasourceName();

    /**
     * Table prefix for JobRunr Control tables.
     * The prefix will be applied to all table names created by the migrations.
     * This is useful for multi-tenant scenarios or when multiple applications share the same database.
     * <p>
     * Note: Table prefix support is planned for a future version.
     * Currently, the table name is fixed to 'jobrunr_control_parameter_sets'.
     */
    Optional<String> tablePrefix();
}
