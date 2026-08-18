package ch.css.jobrunr.control.domain;

import java.util.UUID;

/**
 * Port for querying progress across all processing jobs of a logical batch workflow.
 */
public interface JobWorkflowPort {

    /**
     * Resolves the processing-job progress for the workflow rooted at {@code rootJobId}.
     *
     * @param rootJobId canonical workflow root
     * @return progress across all processing jobs in the workflow
     * @throws IllegalStateException if the workflow root is not a batch job
     */
    BatchProgress resolveProcessingJobProgress(UUID rootJobId);
}
