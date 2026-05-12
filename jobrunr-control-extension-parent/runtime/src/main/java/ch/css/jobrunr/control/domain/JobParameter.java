package ch.css.jobrunr.control.domain;

import java.util.List;

/**
 * Represents a job parameter.
 * Contains metadata such as name, type, required status, and default value.
 *
 * @param name         Parameter name
 * @param type         Parameter data type
 * @param required     Whether parameter is required
 * @param defaultValue Default value as string (null if none)
 * @param enumValues   List of enum values (empty if not an enum type)
 * @param order        Declaration order (0-based index)
 * @param maxlength    Maximum length for string parameters (default 2000, minimal 1)
 * @param sectionId    ID of the section this parameter belongs to (null if none)
 */
public record JobParameter(
        String name,
        String displayName,
        String description,
        JobParameterType type,
        boolean required,
        String defaultValue,
        List<EnumValue> enumValues,
        int order,
        int maxlength,
        String sectionId) {
}
