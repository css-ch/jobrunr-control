package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a set of job parameters stored externally.
 * Used when parameter storage strategy is EXTERNAL.
 */
public record ParameterSet(
        UUID id,
        String jobType,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant lastAccessedAt
) {
    public ParameterSet {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (jobType == null || jobType.isBlank()) throw new IllegalArgumentException("jobType must not be blank");
        if (parameters == null) throw new IllegalArgumentException("parameters must not be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");
    }

    /**
     * Creates a new parameter set with current timestamp.
     */
    public static ParameterSet create(UUID id, String jobType, Map<String, Object> parameters) {
        Instant now = Instant.now();
        return new ParameterSet(id, jobType, parameters, now, now);
    }

    /**
     * Updates last accessed timestamp.
     */
    public ParameterSet markAccessed() {
        return new ParameterSet(id, jobType, parameters, createdAt, Instant.now());
    }
}
