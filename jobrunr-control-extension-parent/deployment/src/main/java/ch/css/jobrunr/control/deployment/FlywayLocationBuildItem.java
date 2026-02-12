package ch.css.jobrunr.control.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to contribute Flyway migration locations.
 * This allows the JobRunr Control extension to automatically configure
 * Flyway locations based on the detected database type.
 */
public final class FlywayLocationBuildItem extends MultiBuildItem {

    private final String location;

    public FlywayLocationBuildItem(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}

