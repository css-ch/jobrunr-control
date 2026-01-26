package ch.css.jobrunr.control.domain;

/**
 * Port for accessing build-time configuration.
 * Provides access to capabilities detected during Quarkus build time.
 */
public interface BuildTimeConfigurationPort {

    /**
     * Checks if SmallRye OpenAPI is available.
     * This capability is detected at build time.
     *
     * @return true if OpenAPI is available, false otherwise
     */
    boolean isOpenApiAvailable();
}
