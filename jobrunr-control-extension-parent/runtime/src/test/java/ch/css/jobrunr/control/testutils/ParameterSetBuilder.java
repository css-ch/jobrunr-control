package ch.css.jobrunr.control.testutils;

import ch.css.jobrunr.control.domain.ParameterSet;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test data builder for ParameterSet.
 * Provides fluent API for creating ParameterSet instances in tests.
 */
public class ParameterSetBuilder {

    private UUID id = UUID.randomUUID();
    private String jobType = "TestJob";
    private Map<String, Object> parameters = new HashMap<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public ParameterSetBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ParameterSetBuilder withJobType(String jobType) {
        this.jobType = jobType;
        return this;
    }

    public ParameterSetBuilder withParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
        return this;
    }

    public ParameterSetBuilder addParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    public ParameterSetBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ParameterSetBuilder withUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ParameterSet build() {
        return new ParameterSet(id, jobType, parameters, createdAt, updatedAt);
    }

    /**
     * Creates a ParameterSet using the factory method.
     */
    public ParameterSet buildWithFactory() {
        return ParameterSet.create(id, jobType, parameters);
    }
}
