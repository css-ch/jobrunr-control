package ch.css.jobrunr.control.jobs.batch.postprocess;


import org.jobrunr.jobs.lambdas.JobRequest;

public record ExampleBatchSuccessRequest(JobRequest jobRequest) implements JobRequest {

    @Override
    public Class<ExampleBatchSuccess> getJobRequestHandler() {
        return ExampleBatchSuccess.class;
    }
}
