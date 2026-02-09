package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a set of job parameters stored externally.
 * Used when parameter storage strategy is EXTERNAL.
 *
 * @param id          Unique identifier
 * @param jobType     Job type name
 * @param parameters  Parameter values map
 * @param createdAt   Creation timestamp
 * @param updatedAt   Last update timestamp
 */
public record ParameterSet(
        UUID id,
        String jobType,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant updatedAt
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
     * Updates the updatedAt timestamp.
     */
    public ParameterSet markUpdated() {
        return new ParameterSet(id, jobType, parameters, createdAt, Instant.now());
    }

    /**
     * Gets a String parameter value.
     */
    public String getString(String name) {
        Object value = parameters.get(name);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets an Integer parameter value.
     */
    public Integer getInteger(String name) {
        Object value = parameters.get(name);
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        return Integer.parseInt(value.toString());
    }

    /**
     * Gets a Boolean parameter value.
     *
     * @param name Parameter name
     * @return Optional containing the boolean value, or empty if parameter is missing or not a boolean
     */
    public Optional<Boolean> getBoolean(String name) {
        Object value = parameters.get(name);
        if (value == null) return Optional.empty();
        if (value instanceof Boolean b) return Optional.of(b);
        try {
            return Optional.of(Boolean.parseBoolean(value.toString()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Gets a Double parameter value.
     */
    public Double getDouble(String name) {
        Object value = parameters.get(name);
        if (value == null) return null;
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    /**
     * Gets a LocalDate parameter value.
     */
    public LocalDate getDate(String name) {
        Object value = parameters.get(name);
        if (value == null) return null;
        if (value instanceof LocalDate d) return d;
        return LocalDate.parse(value.toString());
    }

    /**
     * Gets a LocalDateTime parameter value.
     */
    public LocalDateTime getDateTime(String name) {
        Object value = parameters.get(name);
        if (value == null) return null;
        if (value instanceof LocalDateTime dt) return dt;
        return LocalDateTime.parse(value.toString());
    }

    /**
     * Gets an Enum parameter value.
     */
    public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass) {
        Object value = parameters.get(name);
        if (value == null) return null;
        if (enumClass.isInstance(value)) {
            @SuppressWarnings("unchecked")
            E enumValue = (E) value;
            return enumValue;
        }
        return Enum.valueOf(enumClass, value.toString());
    }

    /**
     * Gets a List of Strings parameter value (for MULTI_ENUM).
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String name) {
        Object value = parameters.get(name);
        if (value == null) return List.of();
        if (value instanceof List<?>) {
            return (List<String>) value;
        }
        // Fallback for comma-separated string
        String strValue = value.toString();
        if (strValue.isBlank()) return List.of();
        return List.of(strValue.split(","));
    }

    /**
     * Gets an EnumSet parameter value (for MULTI_ENUM).
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSet(String name, Class<E> enumClass) {
        List<String> values = getStringList(name);
        if (values.isEmpty()) {
            return EnumSet.noneOf(enumClass);
        }
        EnumSet<E> result = EnumSet.noneOf(enumClass);
        for (String value : values) {
            result.add(Enum.valueOf(enumClass, value.trim()));
        }
        return result;
    }
}
