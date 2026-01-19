package ch.css.jobrunr.control.jobs.calculation;

import org.jobrunr.jobs.lambdas.JobRequest;

public record CalculationBatchJobRequest(Integer totalItems, Integer batchSize,
                                         Boolean simulateErrors) implements JobRequest {
    @Override
    public Class<CalculationBatchJob> getJobRequestHandler() {
        return CalculationBatchJob.class;
    }
}
