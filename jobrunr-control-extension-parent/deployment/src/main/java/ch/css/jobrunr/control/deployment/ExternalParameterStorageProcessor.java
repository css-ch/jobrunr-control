package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.domain.ParameterStorageStrategy;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.logging.Logger;

/**
 * Build processor for external parameter storage validation.
 * Validates that required dependencies (Agroal DataSource) are present when using external parameter storage.
 */
public class ExternalParameterStorageProcessor {

    private static final Logger LOG = Logger.getLogger(ExternalParameterStorageProcessor.class);

    /**
     * Validate that Agroal DataSource is available when using external parameter storage.
     * Logs a warning if the capability is not present, as it's required for JDBC operations.
     */
    @BuildStep
    void validateDataSource(Capabilities capabilities,
                            ParameterStorageBuildTimeConfig config,
                            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (config.strategy() == ParameterStorageStrategy.EXTERNAL) {
            if (!capabilities.isPresent(Capability.AGROAL)) {
                LOG.warnf("External parameter storage is configured but Agroal DataSource is not available. " +
                        "Ensure 'quarkus-agroal' dependency is included and datasource is configured.");
            } else {
                LOG.infof("External parameter storage enabled with JDBC-based storage");
            }
        }
    }
}
