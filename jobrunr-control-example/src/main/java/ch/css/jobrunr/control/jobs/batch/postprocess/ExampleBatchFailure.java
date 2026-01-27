package ch.css.jobrunr.control.jobs.batch.postprocess;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@ApplicationScoped
public class ExampleBatchFailure implements JobRequestHandler<ExampleBatchFailureRequest> {
    private static final Logger log = Logger.getLogger(ExampleBatchFailure.class);

    @Override
    @Job(name = "Example Batch Failure Post-Processing Job", retries = 0)
    public void run(ExampleBatchFailureRequest jobRequest) {
        log.infof("Starting example batch failure job. Parent job id: %s", jobContext().getAwaitedJob());
    }
}
