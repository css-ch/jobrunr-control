package ch.css.jobrunr.control.domain;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a job template with type and parameters.
 * This serves as the basis for scheduling jobs.
 */
public record JobDefinition(
        String type,
        Boolean isBatch,
        Set<JobParameter> parameters
) {
    public JobDefinition {
        Objects.requireNonNull(type, "Type must not be null");
        parameters = parameters != null ? Set.copyOf(parameters) : Collections.emptySet();
    }
}
