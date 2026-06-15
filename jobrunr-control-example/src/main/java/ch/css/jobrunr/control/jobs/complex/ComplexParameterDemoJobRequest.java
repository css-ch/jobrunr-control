package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobParameterSet;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@JobParameterSet(parameterSetClass = ComplexParameterDemoJobParameter.class)
public record ComplexParameterDemoJobRequest() implements JobRequest {
    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return ComplexParameterDemoJob.class;
    }
}
