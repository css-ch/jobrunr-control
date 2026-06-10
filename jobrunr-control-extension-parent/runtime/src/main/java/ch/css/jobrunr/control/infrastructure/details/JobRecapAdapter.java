package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.details.JobRecapService;
import ch.css.jobrunr.control.domain.details.JobRecapStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JobRecapAdapter implements JobRecapService {


    private final JobRecapStoragePort jobRecapStorage;

    @Inject
    public JobRecapAdapter(JobRecapStoragePort jobRecapStorage) {
        this.jobRecapStorage = jobRecapStorage;
    }

    @Override
    public void writeRecap(Map<String, Long> recap) {
        UUID batchJobId = ThreadLocalJobContext.getJobContext().getAwaitedJobId();
        UUID jobId = ThreadLocalJobContext.getJobContext().getJobId();
        if(batchJobId == null || jobId == null) {
            throw new IllegalStateException("Cannot write job recap, because there is no job and child-job context available. Are you sure you are calling this method from a JobRunr child-job?");
        }
        jobRecapStorage.writeRecap(batchJobId, jobId, recap);
    }
}
