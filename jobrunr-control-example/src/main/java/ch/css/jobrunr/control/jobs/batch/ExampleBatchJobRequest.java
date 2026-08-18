package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import org.jobrunr.jobs.lambdas.JobRequest;

public record ExampleBatchJobRequest(
        @JobParameterDefinition(required = false, defaultValue = "100") Integer numberOfChunks,
        Integer chunkSize,
        ExampleBatchJobProcessScenario processScenario) implements JobRequest {

    @Override
    public Class<ExampleBatchJob> getJobRequestHandler() {
        return ExampleBatchJob.class;
    }
}
