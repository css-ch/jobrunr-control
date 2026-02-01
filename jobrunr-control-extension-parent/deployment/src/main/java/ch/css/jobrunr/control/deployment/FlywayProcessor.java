package ch.css.jobrunr.control.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.logging.Logger;

/**
 * Build processor for JobRunr Control Flyway integration.
 * Configures Flyway migrations for the parameter storage tables.
 */
public class FlywayProcessor {

    private static final Logger LOG = Logger.getLogger(FlywayProcessor.class);
    private static final String FEATURE_NAME = "jobrunr-control-flyway";

    /**
     * Register the Flyway feature and validate configuration.
     * The actual Flyway configuration is done via application.properties.
     */
    @BuildStep
    FeatureBuildItem feature(Capabilities capabilities, FlywayBuildTimeConfig config) {
        if (!config.enabled()) {
            LOG.info("JobRunr Control Flyway migrations are disabled");
            return null;
        }

        if (!capabilities.isPresent(Capability.FLYWAY)) {
            LOG.warn("JobRunr Control Flyway is enabled but Flyway extension is not present. " +
                    "Add 'quarkus-flyway' dependency or set 'jobrunr.control.flyway.enabled=false'");
            return null;
        }

        String tablePrefix = config.tablePrefix().orElse("");
        LOG.info("JobRunr Control Flyway migrations enabled with table prefix: '" + tablePrefix + "'");

        return new FeatureBuildItem(FEATURE_NAME);
    }
}
