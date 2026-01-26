package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for triggering a job with optional parameters.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "jobId": "550e8400-e29b-41d4-a716-446655440000",
 *   "parameters": {
 *     "param1": "value1",
 *     "param2": "value2"
 *   }
 * }
 * </pre>
 */
public record TriggerJobRequestDTO(
        UUID jobId,
        Map<String, Object> parameters
) {
}
