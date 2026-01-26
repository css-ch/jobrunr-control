package ch.css.jobrunr.control.jobs.batch.postprocess;

import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExampleBatchSuccess implements JobRequestHandler<ExampleBatchSuccessRequest> {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(ExampleBatchFailure.class);

    @Override
    public void run(ExampleBatchSuccessRequest jobRequest) {
        LOGGER.info("Starting example batch failure job. Parent job id: {}", jobContext().getAwaitedJob());
    }
}
