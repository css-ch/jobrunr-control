package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.UUID;

/**
 * Response DTO for starting a job.
 *
 * @param jobId   ID of the started job
 * @param message Success message
 */
public record StartJobResponse(
        UUID jobId,
        String message
) {
}
