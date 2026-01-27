package ch.css.jobrunr.control.jobs.batch.postprocess;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@ApplicationScoped
public class ExampleBatchSuccess implements JobRequestHandler<ExampleBatchSuccessRequest> {

    private static final Logger log = Logger.getLogger(ExampleBatchSuccess.class);

    @Job(name = "Example Batch Success Post-Processing Job", retries = 0)
    @Override
    public void run(ExampleBatchSuccessRequest jobRequest) {
        log.infof("Starting example batch success job. Parent job id: %s", jobContext().getAwaitedJob());
    }
}
