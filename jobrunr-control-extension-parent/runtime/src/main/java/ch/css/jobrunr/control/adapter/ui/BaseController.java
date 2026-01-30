package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base controller providing common HTMX utilities and helper methods.
 * Reduces duplicate code across controllers by centralizing common patterns.
 */
public abstract class BaseController {

    /**
     * Builds a response that closes the modal and returns the updated content.
     * Uses HX-Trigger header to signal modal closure to HTMX.
     *
     * @param content the template instance to return
     * @return response with modal close trigger
     */
    protected Response buildModalCloseResponse(TemplateInstance content) {
        return Response.ok(content)
                .header("HX-Trigger", "closeModal")
                .build();
    }

    /**
     * Builds an error response that displays in the modal's alert area.
     * The modal stays open so the user can fix the error.
     * Uses HTMX out-of-band (OOB) swap to reliably target the alert container.
     *
     * @param errorMessage the error message to display
     * @return response with error alert HTML
     */
    protected Response buildErrorResponse(String errorMessage) {
        String errorHtml = String.format(
                "<div id=\"form-alerts\" hx-swap-oob=\"true\">" +
                        "<div class=\"alert alert-danger alert-dismissible fade show\" role=\"alert\">" +
                        "<i class=\"bi bi-exclamation-triangle-fill\"></i> <strong>Error:</strong> %s" +
                        "<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"alert\" aria-label=\"Close\"></button>" +
                        "</div>" +
                        "</div>",
                errorMessage
        );
        return Response.ok(errorHtml)
                .header("HX-Trigger", "scrollToError")
                .build();
    }

    /**
     * Parses a scheduled time string into an Instant.
     * Converts LocalDateTime from form input to Instant using system default timezone.
     *
     * @param scheduledAt the datetime string from the form (ISO format)
     * @return the parsed Instant, or null if input is blank
     */
    protected Instant parseScheduledTime(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isBlank()) {
            return null;
        }
        LocalDateTime ldt = LocalDateTime.parse(scheduledAt);
        return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Extracts parameter map from form data.
     * Handles both single-value and multi-value parameters (e.g., from multiselect).
     * Multi-value parameters are joined with commas.
     *
     * @param allFormParams the form parameters from the request
     * @return map of parameter names to values
     */
    protected Map<String, String> extractParameterMap(MultivaluedMap<String, String> allFormParams) {
        return allFormParams.keySet().stream()
                .collect(HashMap::new,
                        (map, key) -> {
                            List<String> values = allFormParams.get(key);
                            if (values != null && values.size() > 1) {
                                // Multiple values (e.g., from multiselect) - join with comma
                                map.put(key, String.join(",", values));
                            } else {
                                // Single value
                                map.put(key, allFormParams.getFirst(key));
                            }
                        },
                        HashMap::putAll);
    }

    /**
     * Truncates large parameter values to prevent HTTP 413 errors.
     * String values longer than MAX_STRING_LENGTH are truncated.
     * Collection/Map sizes are limited to prevent excessive data transfer.
     *
     * @param parameters the parameters to truncate
     * @return truncated parameter map
     */
    protected Map<String, Object> truncateParameterValues(Map<String, Object> parameters) {
        final int MAX_STRING_LENGTH = 1000;
        final int MAX_COLLECTION_SIZE = 100;

        Map<String, Object> truncated = new HashMap<>();

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String str) {
                if (str.length() > MAX_STRING_LENGTH) {
                    truncated.put(entry.getKey(), str.substring(0, MAX_STRING_LENGTH) + "... [truncated]");
                } else {
                    truncated.put(entry.getKey(), value);
                }
            } else if (value instanceof java.util.Collection<?> collection) {
                if (collection.size() > MAX_COLLECTION_SIZE) {
                    truncated.put(entry.getKey(), String.format("[Collection with %d items - too large to display]", collection.size()));
                } else {
                    truncated.put(entry.getKey(), value);
                }
            } else if (value instanceof Map<?, ?> map) {
                if (map.size() > MAX_COLLECTION_SIZE) {
                    truncated.put(entry.getKey(), String.format("[Map with %d entries - too large to display]", map.size()));
                } else {
                    truncated.put(entry.getKey(), value);
                }
            } else {
                truncated.put(entry.getKey(), value);
            }
        }

        return truncated;
    }

    /**
     * Validates that a required string field is not null or blank.
     *
     * @param value     the value to validate
     * @param fieldName the name of the field (for error messages)
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
