package ch.css.jobrunr.control.domain;

import java.util.List;

/**
 * Represents a job parameter.
 * Contains metadata such as name, type, required status, and default value.
 */
public record JobParameter(
        String name,
        JobParameterType type,
        boolean required,
        String defaultValue,
        List<String> enumValues) {
}
