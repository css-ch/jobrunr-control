package ch.css.jobrunr.control.jobs.calculation;

import org.jobrunr.jobs.lambdas.JobRequest;

public record CalculationItemProcessorRequest(int itemToCompute, Boolean simulateErrors) implements JobRequest {

    @Override
    public Class<CalculationItemProcessor> getJobRequestHandler() {
        return CalculationItemProcessor.class;
    }
}
