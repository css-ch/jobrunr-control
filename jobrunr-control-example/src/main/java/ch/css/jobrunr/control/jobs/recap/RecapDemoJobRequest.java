package ch.css.jobrunr.control.jobs.recap;

import ch.css.jobrunr.control.annotations.JobParameterSet;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@JobParameterSet(parameterSetClass = RecapDemoJobParameter.class)
public record RecapDemoJobRequest() implements JobRequest {
    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return RecapDemoJob.class;
    }
}
