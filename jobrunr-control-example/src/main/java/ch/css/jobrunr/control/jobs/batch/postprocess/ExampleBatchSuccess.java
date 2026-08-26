package ch.css.jobrunr.control.jobs.batch.postprocess;

import ch.css.jobrunr.control.domain.BusinessStatus;
import ch.css.jobrunr.control.domain.JobResultPort;
import ch.css.jobrunr.control.domain.exceptions.JobProcessingException;
import ch.css.jobrunr.control.jobs.batch.ExampleBatchJobProcessScenario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.StorageProvider;

import java.util.UUID;

@ApplicationScoped
public class ExampleBatchSuccess implements JobRequestHandler<ExampleBatchSuccessRequest> {

    private static final Logger LOG = Logger.getLogger(ExampleBatchSuccess.class);

    private final JobResultPort jobResultPort;
    private final StorageProvider storageProvider;

    @Inject
    public ExampleBatchSuccess(JobResultPort jobResultPort, StorageProvider storageProvider) {
        this.jobResultPort = jobResultPort;
        this.storageProvider = storageProvider;
    }

    @Override
    public void run(ExampleBatchSuccessRequest jobRequest) {
        try {
            UUID workflowRootJobId = jobRequest.workflowRootJobId();
            LOG.infof("Starting example batch success job. Workflow root job id: %s", workflowRootJobId);
            ThreadLocalJobContext.getJobContext().logger().info(
                    String.format("Starting success post-processing for workflow %s", workflowRootJobId));

            // Get root metadata for the detailed result message.
            var workflowRootJob = storageProvider.getJobById(workflowRootJobId);
            Integer totalChunks = (Integer) workflowRootJob.getMetadata().get("total_chunks");
            String enqueuedAt = (String) workflowRootJob.getMetadata().get("enqueued_at");

            try {
                Thread.sleep(5_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobProcessingException("Example batch post-processing was interrupted", e);
            }

            if (jobRequest.processScenario() == ExampleBatchJobProcessScenario.POSTJOB_ERROR) {
                ThreadLocalJobContext.getJobContext().logger().error("Simulated post-processing error");
                throw new JobProcessingException("Simulated post-processing error");
            }

            // Build detailed success message
            String resultMessage = String.format(
                    "Batch job completed successfully - all %d chunk jobs processed. Enqueued at: %s",
                    totalChunks != null ? totalChunks : 0,
                    enqueuedAt != null ? enqueuedAt : "unknown"
            );

            // Store the logical execution result on the canonical workflow root.
            jobResultPort.storeResult(0, resultMessage);
            jobResultPort.setBusinessStatus(BusinessStatus.SUCCESS);

            ThreadLocalJobContext.getJobContext().logger().info(resultMessage);
            LOG.infof("Batch success result stored: %s", resultMessage);
        } catch (Exception e) {
            LOG.error("Error during example batch success job", e);
            jobResultPort.setBusinessStatus(BusinessStatus.NONE);
            jobResultPort.storeResult(50, "Error during example batch success job: " + e.getMessage());
            throw e;
        }
    }
}
