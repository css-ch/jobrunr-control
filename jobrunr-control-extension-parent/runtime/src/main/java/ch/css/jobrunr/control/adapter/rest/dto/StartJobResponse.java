package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.UUID;

/**
 * Response DTO for starting a job.
 */
public record StartJobResponse(
        UUID jobId,
        String message
) {
}
