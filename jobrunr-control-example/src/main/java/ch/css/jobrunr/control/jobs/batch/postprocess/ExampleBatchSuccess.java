package ch.css.jobrunr.control.jobs.batch.postprocess;

import ch.css.jobrunr.control.domain.JobResultPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.annotations.Job;
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

    @Job(name = "Example Batch Success Post-Processing Job", retries = 0)
    @Override
    public void run(ExampleBatchSuccessRequest jobRequest) {
        UUID parentJobId = ThreadLocalJobContext.getJobContext().getAwaitedJob();
        LOG.infof("Starting example batch success job. Parent job id: %s", parentJobId);

        // Get parent job metadata for detailed result message
        var parentJob = storageProvider.getJobById(parentJobId);
        Integer totalChildren = (Integer) parentJob.getMetadata().get("total_children");
        String enqueuedAt = (String) parentJob.getMetadata().get("enqueued_at");

        // Build detailed success message
        String resultMessage = String.format(
                "Batch job completed successfully - all %d child jobs processed. Enqueued at: %s",
                totalChildren != null ? totalChildren : 0,
                enqueuedAt != null ? enqueuedAt : "unknown"
        );

        // Store result - automatically goes to parent job since we're in a continuation job
        jobResultPort.storeResult(0, resultMessage);

        LOG.infof("Batch success result stored: %s", resultMessage);
    }
}
