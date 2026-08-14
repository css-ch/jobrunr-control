package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.details.JobRecapService;
import ch.css.jobrunr.control.domain.details.JobRecapStoragePort;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobWorkflowResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JobRecapAdapter implements JobRecapService {


    private final JobRecapStoragePort jobRecapStorage;
    private final JobWorkflowResolver jobWorkflowResolver;

    @Inject
    public JobRecapAdapter(JobRecapStoragePort jobRecapStorage, JobWorkflowResolver jobWorkflowResolver) {
        this.jobRecapStorage = jobRecapStorage;
        this.jobWorkflowResolver = jobWorkflowResolver;
    }

    @Override
    public void writeRecap(Map<String, Long> recap) {
        UUID jobId = ThreadLocalJobContext.getJobContext().getJobId();
        if (jobId == null) {
            throw new IllegalStateException("Cannot write job recap outside a JobRunr job context");
        }
        UUID rootJobId = jobWorkflowResolver.resolveRootIdFromContext();
        jobRecapStorage.writeRecap(rootJobId, jobId, recap);
    }
}
