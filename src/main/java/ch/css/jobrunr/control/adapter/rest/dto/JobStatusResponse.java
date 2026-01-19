package ch.css.jobrunr.control.adapter.rest.dto;

import ch.css.jobrunr.control.domain.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for job status check.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobStatusResponse(
        String jobId,
        String jobName,
        String jobType,
        JobStatus status,
        String startedAt,
        String finishedAt,
        BatchProgressDTO batchProgress
) {
}
