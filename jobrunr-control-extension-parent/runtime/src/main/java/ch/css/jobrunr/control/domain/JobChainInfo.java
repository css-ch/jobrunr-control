package ch.css.jobrunr.control.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Information about a job in a job chain.
 * Used to track jobs at the end of a job tree (continuation or failure handlers).
 *
 * @param jobId            The unique identifier of the job
 * @param status           The current status of the job
 * @param awaitingOnStates The states the parent job must be in for this job to execute (e.g., SUCCEEDED for continueWith, FAILED for onFailure)
 * @param startedAt        When the job started execution (null if not started)
 * @param finishedAt       When the job finished execution (null if not finished)
 * @param duration         How long the job took to execute (null if not finished)
 */
public record JobChainInfo(
        UUID jobId,
        JobStatus status,
        Set<JobAwaitingState> awaitingOnStates,
        Instant startedAt,
        Instant finishedAt,
        Duration duration
) {
}
