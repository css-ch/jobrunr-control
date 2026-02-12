package ch.css.jobrunr.control.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import org.jboss.logging.Logger;

/**
 * Build processor for JobRunr Control Flyway integration.
 * Validates Flyway configuration and registers migration resources.
 */
public class FlywayProcessor {

    private static final Logger LOG = Logger.getLogger(FlywayProcessor.class);
    private static final String FEATURE_NAME = "jobrunr-control-flyway";

    /**
     * Register the Flyway feature and automatically configure migration locations.
     */
    @BuildStep
    FeatureBuildItem feature(
            Capabilities capabilities,
            ch.css.jobrunr.control.deployment.FlywayBuildTimeConfig config,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources) {

        if (!config.enabled()) {
            LOG.info("JobRunr Control Flyway migrations are disabled");
            return null;
        }

        if (!capabilities.isPresent(Capability.FLYWAY)) {
            LOG.warnf("JobRunr Control Flyway is enabled but Flyway extension is not present. " +
                    "Add 'quarkus-flyway' dependency or set 'quarkus.jobrunr-control.flyway.enabled=false'");
            return null;
        }

        // Get database type - REQUIRED when enabled
        String dbType = config.dbType()
                .orElseThrow(() -> new ConfigurationException(
                        "quarkus.jobrunr-control.flyway.db-type is required when " +
                                "quarkus.jobrunr-control.flyway.enabled=true. " +
                                "Supported values: postgresql, h2, oracle"));

        dbType = dbType.toLowerCase();
        String tablePrefix = config.tablePrefix().orElse("");

        // Validate database type
        if (!isValidDatabaseType(dbType)) {
            throw new ConfigurationException(
                    String.format("Unsupported database type '%s'. Supported values: postgresql, h2, oracle", dbType));
        }

        LOG.infof("JobRunr Control Flyway migrations enabled for database type '%s' with table prefix: '%s'",
                dbType, tablePrefix);

        // Validate migration scripts exist
        validateMigrationPath(dbType);

        // Register migration resources for native image
        registerNativeImageResources(dbType, nativeImageResources);

        return new FeatureBuildItem(FEATURE_NAME);
    }

    /**
     * Register migration scripts as native image resources.
     */
    private void registerNativeImageResources(String dbType, BuildProducer<NativeImageResourceBuildItem> producer) {
        // Register JobRunr Control migration
        producer.produce(new NativeImageResourceBuildItem(
                "db/migration/jobrunr-control/" + dbType + "/V1.0.0__create_parameter_sets_table.sql"));

        LOG.debugf("Registered native image resources for database type: %s", dbType);
    }

    /**
     * Validate that database type is supported.
     */
    private boolean isValidDatabaseType(String dbType) {
        return dbType.equals("postgresql") || dbType.equals("h2") || dbType.equals("oracle");
    }

    /**
     * Validate that the migration path exists for the specified database type.
     */
    private void validateMigrationPath(String dbType) {
        String migrationPath = "db/migration/jobrunr-control/" + dbType + "/V1.0.0__create_parameter_sets_table.sql";

        if (FlywayProcessor.class.getClassLoader().getResource(migrationPath) == null) {
            throw new ConfigurationException(
                    String.format("Migration script not found for database type '%s' at path '%s'. " +
                            "Supported database types: postgresql, h2, oracle", dbType, migrationPath));
        }
    }
}
