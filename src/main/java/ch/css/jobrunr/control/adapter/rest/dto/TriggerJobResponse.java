package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.UUID;

/**
 * Response DTO for triggering a job.
 */
public record TriggerJobResponse(
        UUID jobId,
        String message
) {
}
