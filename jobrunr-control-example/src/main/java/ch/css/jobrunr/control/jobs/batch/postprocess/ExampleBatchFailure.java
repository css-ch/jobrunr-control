package ch.css.jobrunr.control.jobs.batch.postprocess;

import ch.css.jobrunr.control.domain.BusinessStatus;
import ch.css.jobrunr.control.domain.JobResultPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.StorageProvider;

import java.util.UUID;

@ApplicationScoped
public class ExampleBatchFailure implements JobRequestHandler<ExampleBatchFailureRequest> {
    private static final Logger LOG = Logger.getLogger(ExampleBatchFailure.class);

    private final JobResultPort jobResultPort;
    private final StorageProvider storageProvider;

    @Inject
    public ExampleBatchFailure(JobResultPort jobResultPort, StorageProvider storageProvider) {
        this.jobResultPort = jobResultPort;
        this.storageProvider = storageProvider;
    }

    @Override
    @Transactional
    public void run(ExampleBatchFailureRequest jobRequest) {
        UUID workflowRootJobId = jobRequest.workflowRootJobId();
        LOG.infof("Starting example batch failure job. Workflow root job id: %s", workflowRootJobId);
        ThreadLocalJobContext.getJobContext().logger().info(
                String.format("Starting failure post-processing for workflow %s", workflowRootJobId));

        // Get root metadata for the detailed result message.
        var workflowRootJob = storageProvider.getJobById(workflowRootJobId);
        Integer totalChunks = (Integer) workflowRootJob.getMetadata().get("total_chunks");
        String enqueuedAt = (String) workflowRootJob.getMetadata().get("enqueued_at");

        try {
            Thread.sleep(5_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Example batch failure post-processing was interrupted", e);
        }

        // Build detailed failure message
        String resultMessage = String.format(
                "Batch job failed - one or more chunk jobs encountered errors. Total chunks: %d, Enqueued at: %s",
                totalChunks != null ? totalChunks : 0,
                enqueuedAt != null ? enqueuedAt : "unknown"
        );

        // Store the logical execution result on the canonical workflow root.
        jobResultPort.storeResult(1, resultMessage);
        jobResultPort.setBusinessStatus(BusinessStatus.WARNING);

        ThreadLocalJobContext.getJobContext().logger().error(resultMessage);
        LOG.errorf("Batch failure result stored: %s", resultMessage);
    }
}
