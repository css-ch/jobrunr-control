package ch.css.jobrunr.control.application.validation;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import ch.css.jobrunr.control.domain.exceptions.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Validator for job parameters.
 * Validates types, required fields, and value conversion.
 */
@ApplicationScoped
public class JobParameterValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Inject
    public JobParameterValidator(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Validates the provided parameters against the job definition.
     *
     * @param jobDefinition Job definition with expected parameters
     * @param parameters    Actually provided parameters
     * @throws ValidationException when validation errors occur
     */
    public Map<String, Object> convertAndValidate(JobDefinition jobDefinition, Map<String, String> parameters) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> convertedParams = new HashMap<>();

        for (JobParameter param : jobDefinition.parameters()) {
            String value = parameters.get("parameters." + param.name());

            // Check required fields
            if (param.required() && (value == null || value.isBlank())) {
                errors.add("Parameter '" + param.name() + "' is required");
                continue;
            }

            // Skip further validation for optional null values
            if (value == null || value.isBlank()) {
                continue;
            }

            // Validate and convert type
            try {
                convertedParams.put(param.name(), convertAndValidate(param.name(), param.type(), value));
            } catch (ValidationException e) {
                errors.addAll(e.getErrors());
            }
        }

        // Additional validation for JobRequest parameters
        try {
            Class<?> parametersClass;
            if(jobDefinition.usesExternalParameters()) {
                parametersClass = Thread.currentThread().getContextClassLoader().loadClass(jobDefinition.externalParametersClassName());
            } else {
                parametersClass = Thread.currentThread().getContextClassLoader().loadClass(jobDefinition.jobRequestTypeName());
            }
            Object parameterSet = objectMapper.convertValue(convertedParams, parametersClass);
            final Set<ConstraintViolation<Object>> violations = validator.validate(parameterSet);
            if (!violations.isEmpty()) {
                errors.addAll(violations.stream().map(ConstraintViolation::getMessage).toList());
            }
        } catch (ClassNotFoundException e) {
            throw new ValidationException("Validation failed: JobRequest class '" + jobDefinition.jobRequestTypeName() + "' not found");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        return convertedParams;
    }

    /**
     * Validates and converts a single parameter value.
     *
     * @param name  Parameter name
     * @param type  Expected type
     * @param value Value to validate
     * @return Converted value
     * @throws ValidationException when the value does not match the type
     */
    private Object convertAndValidate(String name, JobParameterType type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return switch (type) {
                case STRING, ENUM, MULTILINE -> convertToString(value);
                case INTEGER -> convertToInteger(name, value);
                case DOUBLE -> convertToDouble(name, value);
                case BOOLEAN -> convertToBoolean(name, value);
                case DATE -> convertToDate(name, value);
                case DATETIME -> convertToDateTime(name, value);
                case MULTI_ENUM -> convertToStringList(value);
            };
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Parameter '" + name + "': " + e.getMessage());
        }
    }


    private String convertToString(Object value) {
        return value.toString();
    }

    private List<String> convertToStringList(Object value) {
        if (value instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<String> stringList = (List<String>) list;
            return stringList;
        }
        String strValue = value.toString();
        if (strValue.isBlank()) {
            return List.of();
        }
        // Split by comma and trim whitespace
        return java.util.stream.Stream.of(strValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Integer convertToInteger(String name, Object value) {
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new ValidationException("Parameter '" + name + "' muss eine ganze Zahl sein");
        }
    }

    private Double convertToDouble(String name, Object value) {
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            throw new ValidationException("Parameter '" + name + "' muss eine Zahl sein");
        }
    }

    private Boolean convertToBoolean(String name, Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String strValue = value.toString().toLowerCase();
        if ("true".equals(strValue) || "false".equals(strValue)) {
            return Boolean.parseBoolean(strValue);
        }
        throw new ValidationException("Parameter '" + name + "' muss 'true' oder 'false' sein");
    }

    private LocalDate convertToDate(String name, Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "Parameter '" + name + "' muss ein gültiges Datum im Format YYYY-MM-DD sein"
            );
        }
    }

    private LocalDateTime convertToDateTime(String name, Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "Parameter '" + name + "' muss ein gültiges Datum-Zeit im Format YYYY-MM-DDTHH:mm:ss sein"
            );
        }
    }
}

