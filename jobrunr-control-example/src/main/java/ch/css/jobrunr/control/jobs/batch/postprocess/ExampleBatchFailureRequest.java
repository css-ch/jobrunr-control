package ch.css.jobrunr.control.jobs.batch.postprocess;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record ExampleBatchFailureRequest(UUID workflowRootJobId) implements JobRequest {

    @Override
    public Class<ExampleBatchFailure> getJobRequestHandler() {
        return ExampleBatchFailure.class;
    }
}
