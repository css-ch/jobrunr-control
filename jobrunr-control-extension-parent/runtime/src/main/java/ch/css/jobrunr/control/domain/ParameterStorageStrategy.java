package ch.css.jobrunr.control.domain;

/**
 * Defines strategies for storing job parameters.
 */
public enum ParameterStorageStrategy {
    /**
     * Parameters stored inline in JobRunr job table (default).
     */
    INLINE,

    /**
     * Parameters stored in separate repository with reference ID in job.
     */
    EXTERNAL
}
