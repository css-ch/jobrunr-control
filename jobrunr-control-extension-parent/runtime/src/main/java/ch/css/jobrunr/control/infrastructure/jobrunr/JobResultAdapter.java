package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.JobResultPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.StorageProvider;

import java.util.UUID;

/**
 * JobRunr-based implementation of JobResultPort.
 * <p>
 * Stores job results in JobRunr's storage by manipulating job metadata.
 * Automatically detects if running in a continuation job and stores results in the parent job.
 *
 * <pre>{@code
 * @ApplicationScoped
 * public class MySuccessJob implements JobRequestHandler<MySuccessRequest> {
 *
 *     @Inject
 *     JobResultPort jobResultPort;
 *
 *     @Override
 *     public void run(MySuccessRequest request) {
 *         // Automatically stores in parent job if this is a continuation job
 *         jobResultPort.storeResult(0, "42 items processed successfully");
 *     }
 * }
 * }</pre>
 */
@ApplicationScoped
public class JobResultAdapter implements JobResultPort {

    /**
     * Metadata key used to store the result message in the JobRunr job.
     */
    public static final String RESULT_METADATA_KEY = "jobrunr-control-result";

    /**
     * Metadata key used to store the result code in the JobRunr job.
     */
    public static final String RESULT_CODE_METADATA_KEY = "jobrunr-control-result-code";

    private static final Logger LOG = Logger.getLogger(JobResultAdapter.class);

    private final StorageProvider storageProvider;

    @Inject
    public JobResultAdapter(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public void storeResult(int resultCode, String result) {
        try {
            UUID parentJobId = ThreadLocalJobContext.getJobContext().getAwaitedJob();
            if (parentJobId != null) {
                // Running in a continuation job - store in parent
                storeResultInJob(parentJobId, resultCode, result);
                LOG.debugf("Stored result in parent job %s: resultCode=%d, result=%s", parentJobId, resultCode, result);
            } else {
                // Regular job - store in current job
                ThreadLocalJobContext.getJobContext().saveMetadata(RESULT_CODE_METADATA_KEY, resultCode);
                ThreadLocalJobContext.getJobContext().saveMetadata(RESULT_METADATA_KEY, result);
                LOG.debugf("Stored result in current job: resultCode=%d, result=%s", resultCode, result);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to store job result – not running inside a JobRunr job context?");
        }
    }

    /**
     * Internal helper method to store result in a specific job by UUID.
     */
    private void storeResultInJob(UUID jobId, int resultCode, String result) {
        try {
            var job = storageProvider.getJobById(jobId);
            job.getMetadata().put(RESULT_CODE_METADATA_KEY, resultCode);
            job.getMetadata().put(RESULT_METADATA_KEY, result);
            storageProvider.save(job);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to store result in job %s", jobId);
            throw new IllegalArgumentException("Failed to store result in job " + jobId, e);
        }
    }
}

