package ch.css.jobrunr.control.domain;

/**
 * Enum for job status.
 * Represents the various states of a job during its lifecycle.
 */
public enum JobStatus {
    ENQUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED
}

