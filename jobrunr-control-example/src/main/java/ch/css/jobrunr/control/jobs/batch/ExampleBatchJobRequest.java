package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import org.jobrunr.jobs.lambdas.JobRequest;

public record ExampleBatchJobRequest(
        @JobParameterDefinition(defaultValue = "100") Integer numberOfJunks,
        Integer junkSize,
        @JobParameterDefinition(defaultValue = "true") Boolean simulateErrors) implements JobRequest {
    @Override
    public Class<ExampleBatchJob> getJobRequestHandler() {
        return ExampleBatchJob.class;
    }
}
