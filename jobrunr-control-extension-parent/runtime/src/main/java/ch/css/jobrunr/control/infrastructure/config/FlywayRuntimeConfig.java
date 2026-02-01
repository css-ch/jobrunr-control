package ch.css.jobrunr.control.infrastructure.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * Runtime configuration for JobRunr Control Flyway migrations.
 */
@ConfigMapping(prefix = "jobrunr.control.flyway")
public interface FlywayRuntimeConfig {

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
