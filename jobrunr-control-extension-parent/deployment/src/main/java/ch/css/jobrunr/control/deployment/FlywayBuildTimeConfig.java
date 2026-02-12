package ch.css.jobrunr.control.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Build-time configuration for JobRunr Control Flyway migrations.
 */
@ConfigMapping(prefix = "quarkus.jobrunr-control.flyway")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface FlywayBuildTimeConfig {

    /**
     * Enable or disable Flyway migrations for JobRunr Control.
     * When enabled, the extension automatically configures Flyway locations based on db-type.
     * Default: false
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Database type for migrations.
     * REQUIRED when flyway.enabled=true.
     * Supported values: postgresql, h2, oracle
     * <p>
     * This determines which migration scripts to use and automatically configures:
     * - JobRunr Pro migrations: classpath:org/jobrunr/database/migrations/{db-type}
     * - JobRunr Control migrations: classpath:db/migration/jobrunr-control/{db-type}
     */
    Optional<String> dbType();

    /**
     * Named datasource to use for migrations.
     * If not set, uses the default datasource.
     * This allows using a different connection with elevated privileges for migrations.
     * Default: &lt;default&gt;
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
     * Default: empty (no prefix)
     */
    Optional<String> tablePrefix();
}
