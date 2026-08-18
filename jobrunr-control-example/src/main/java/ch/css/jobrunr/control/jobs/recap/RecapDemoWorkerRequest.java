package ch.css.jobrunr.control.jobs.recap;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public record RecapDemoWorkerRequest(int number, boolean exception) implements JobRequest {
    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return RecapDemoWorker.class;
    }
}
