package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for cloning and starting a job with optional parameter overrides.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "cloneFromId": "550e8400-e29b-41d4-a716-446655440000",
 *   "suffix": "20260126",
 *   "parameters": {
 *     "param1": "value1",
 *     "param2": "value2"
 *   }
 * }
 * </pre>
 */
public record CloneAndStartJobRequestDTO(
        UUID cloneFromId,
        String suffix,
        Map<String, Object> parameters
) {
}
