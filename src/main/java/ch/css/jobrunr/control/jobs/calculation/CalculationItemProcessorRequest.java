package ch.css.jobrunr.control.jobs.calculation;

import org.jobrunr.jobs.lambdas.JobRequest;

public record CalculationItemProcessorRequest(int itemToCompute, Boolean simulateErrors) implements JobRequest {

    @Override
    public Class<CalculationItemProcessor> getJobRequestHandler() {
        return CalculationItemProcessor.class;
    }

    /**
     * Overrides {@code toString()} to provide a suitable job name in the JobRunr Dashboard, as used in the {@code @Job} annotation.
     */
    @Override
    public String toString() {
        return Integer.toString(itemToCompute);
    }
}
