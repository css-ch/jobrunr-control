package ch.css.jobrunr.control.infrastructure.quarkus;

import ch.css.jobrunr.control.domain.BuildTimeConfigurationPort;
import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionRecorder;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Infrastructure adapter for accessing build-time configuration.
 * Provides access to capabilities detected during Quarkus build time.
 */
@ApplicationScoped
public class BuildTimeConfigurationAdapter implements BuildTimeConfigurationPort {

    @Override
    public boolean isOpenApiAvailable() {
        return JobDefinitionRecorder.JobDefinitionRegistry.INSTANCE.isOpenApiAvailable();
    }
}
