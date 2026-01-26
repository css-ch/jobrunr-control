package ch.css.jobrunr.control.jobs.batch.postprocess;

import org.jobrunr.jobs.lambdas.JobRequest;


public record ExampleBatchFailureRequest(JobRequest jobRequest) implements JobRequest {

    @Override
    public Class<ExampleBatchFailure> getJobRequestHandler() {
        return ExampleBatchFailure.class;
    }
}
