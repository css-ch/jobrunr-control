package ch.css.jobrunr.control.jobs.purejobrunr;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Plain JobRunr handler without the JobRunr Control {@code @ConfigurableJob} annotation.
 * Jobs scheduled through this handler are managed by JobRunr directly and are
 * intentionally invisible to the JobRunr Control dashboard.
 */
@ApplicationScoped
public class PureJobRunrJob implements JobRequestHandler<PureJobRunrJobRequest> {

    private static final Logger LOG = Logger.getLogger(PureJobRunrJob.class);

    @Override
    @Job(name = "Pure JobRunr job: %0")
    public void run(PureJobRunrJobRequest request) {
        LOG.infof("Pure JobRunr job executed with payload: %s", request.payload());
    }
}
