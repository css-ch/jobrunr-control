package ch.css.jobrunr.control.domain;

import java.util.Objects;

/**
 * Represents a job parameter.
 * Contains metadata such as name, type, required status, and default value.
 */
public record JobParameter(
        String name,
        JobParameterType type,
        boolean required,
        Object defaultValue
) {
    public JobParameter {
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(type, "Type must not be null");
    }
}
