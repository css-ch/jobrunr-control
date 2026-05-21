package ch.css.jobrunr.control.jobs.complex;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public record ComplexParameterDemoChildJobRequest(int number) implements JobRequest {
    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return ComplexParameterDemoChildJob.class;
    }
}