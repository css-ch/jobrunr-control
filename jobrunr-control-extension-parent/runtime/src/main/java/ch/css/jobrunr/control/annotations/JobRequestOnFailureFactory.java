package ch.css.jobrunr.control.annotations;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestId;

/**
 * Factory for creating continuation jobs that execute on failure.
 */
public interface JobRequestOnFailureFactory {
    /**
     * Creates a job request to execute when the parent job fails.
     *
     * @param jobRequestId ID of the parent job
     * @param jobRequest   The parent job request
     * @return Job request to execute on failure
     */
    JobRequest createOnFailureJobRequest(JobRequestId jobRequestId, JobRequest jobRequest);
}
