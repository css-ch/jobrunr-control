package ch.css.jobrunr.control.annotations;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestId;

public interface JobRequestOnSuccessFactory {
    JobRequest createOnSuccessJobRequest(JobRequestId jobRequestId, JobRequest jobRequest);
}
