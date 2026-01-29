package ch.css.jobrunr.control.annotations;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestId;

public interface JobRequestOnFailureFactory {
    JobRequest createOnFailureJobRequest(JobRequestId jobRequestId, JobRequest jobRequest);
}
