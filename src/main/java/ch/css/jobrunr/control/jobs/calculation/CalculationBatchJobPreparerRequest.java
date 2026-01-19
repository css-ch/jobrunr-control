package ch.css.jobrunr.control.jobs.calculation;

import org.jobrunr.jobs.lambdas.JobRequest;

public record CalculationBatchJobPreparerRequest(Integer totalItems, Integer batchSize,
                                                 Boolean simulateErrors) implements JobRequest {
    @Override
    public Class<CalculationBatchJobPreparer> getJobRequestHandler() {
        return CalculationBatchJobPreparer.class;
    }
}
