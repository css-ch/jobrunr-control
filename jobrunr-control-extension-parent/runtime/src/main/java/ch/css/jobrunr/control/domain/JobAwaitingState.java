package ch.css.jobrunr.control.domain;

/**
 * Represents the parent job states that trigger a continuation job.
 * This is used in job chains to indicate when a job should execute.
 */
public enum JobAwaitingState {
    /**
     * Job executes when parent succeeds (continueWith).
     */
    SUCCEEDED,

    /**
     * Job executes when parent fails (onFailure).
     */
    FAILED
}
