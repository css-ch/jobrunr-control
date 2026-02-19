package ch.css.jobrunr.control.jobs.batch;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record ExampleBatchJobItemProcessorRequest(int chunkId, int chunkSize,
                                                  Boolean simulateErrors,
                                                  UUID parentBatchJobId) implements JobRequest {

    @Override
    public Class<ExampleBatchJobItemProcessor> getJobRequestHandler() {
        return ExampleBatchJobItemProcessor.class;
    }

    /**
     * Overrides {@code toString()} to provide a suitable job name in the JobRunr Dashboard, as used in the {@code @Job} annotation.
     */
    @Override
    public String toString() {
        return Integer.toString(chunkId);
    }
}
