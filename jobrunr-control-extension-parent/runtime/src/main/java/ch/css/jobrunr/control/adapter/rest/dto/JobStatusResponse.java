package ch.css.jobrunr.control.adapter.rest.dto;

import ch.css.jobrunr.control.domain.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for job status check.
 *
 * @param jobId         Job ID
 * @param jobName       Job name
 * @param jobType       Job type
 * @param status        Current job status
 * @param startedAt     Start time (ISO-8601 format)
 * @param finishedAt    Finish time (ISO-8601 format, null if not finished)
 * @param batchProgress Batch progress (null if not a batch job)
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
