package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.BusinessStatus;
import ch.css.jobrunr.control.domain.JobResultPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    /**
     * Metadata key used to store the business status in the JobRunr job.
     */
    public static final String RESULT_BUSINESS_STATUS_METADATA_KEY = "jobrunr-control-result-business-status";

    private static final Logger LOG = Logger.getLogger(JobResultAdapter.class);

    private final StorageProvider storageProvider;
    private final JobWorkflowResolver jobWorkflowResolver;

    @Inject
    public JobResultAdapter(StorageProvider storageProvider, JobWorkflowResolver jobWorkflowResolver) {
        this.storageProvider = storageProvider;
        this.jobWorkflowResolver = jobWorkflowResolver;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void storeResult(int resultCode, String result) {
        try {
            UUID currentJobId = ThreadLocalJobContext.getJobContext().getJobId();
            UUID rootJobId = jobWorkflowResolver.resolveRootIdFromContext();
            if (!rootJobId.equals(currentJobId)) {
                storeResultInJob(rootJobId, resultCode, result);
                LOG.debugf("Stored result in workflow root %s: resultCode=%d, result=%s", rootJobId, resultCode, result);
            } else {
                ThreadLocalJobContext.getJobContext().saveMetadata(RESULT_CODE_METADATA_KEY, resultCode);
                ThreadLocalJobContext.getJobContext().saveMetadata(RESULT_METADATA_KEY, result);
                LOG.debugf("Stored result in current root job: resultCode=%d, result=%s", resultCode, result);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to store job result – not running inside a JobRunr job context?");
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void setBusinessStatus(BusinessStatus status) {
        try {
            UUID currentJobId = ThreadLocalJobContext.getJobContext().getJobId();
            UUID rootJobId = jobWorkflowResolver.resolveRootIdFromContext();
            if (!rootJobId.equals(currentJobId)) {
                var job = storageProvider.getJobById(rootJobId);
                job.getMetadata().put(RESULT_BUSINESS_STATUS_METADATA_KEY, status);
                storageProvider.save(job);
                LOG.debugf("Stored business status in workflow root %s: %s", rootJobId, status);
            } else {
                ThreadLocalJobContext.getJobContext().saveMetadata(RESULT_BUSINESS_STATUS_METADATA_KEY, status);
                LOG.debugf("Stored business status in current root job: %s", status);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to store business status – not running inside a JobRunr job context?");
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
