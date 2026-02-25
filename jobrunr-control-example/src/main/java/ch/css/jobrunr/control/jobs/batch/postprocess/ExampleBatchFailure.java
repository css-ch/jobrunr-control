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
    @Job(name = "Example Batch Failure Post-Processing Job", retries = 0)
    public void run(ExampleBatchFailureRequest jobRequest) {
        UUID parentJobId = ThreadLocalJobContext.getJobContext().getAwaitedJob();
        LOG.infof("Starting example batch failure job. Parent job id: %s", parentJobId);

        // Get parent job metadata for detailed result message
        var parentJob = storageProvider.getJobById(parentJobId);
        Integer totalChildren = (Integer) parentJob.getMetadata().get("total_children");
        String enqueuedAt = (String) parentJob.getMetadata().get("enqueued_at");

        // Build detailed failure message
        String resultMessage = String.format(
                "Batch job failed - one or more child jobs encountered errors. Total children: %d, Enqueued at: %s",
                totalChildren != null ? totalChildren : 0,
                enqueuedAt != null ? enqueuedAt : "unknown"
        );

        // Store result - automatically goes to parent job since we're in a continuation job
        jobResultPort.storeResult(1, resultMessage);

        LOG.errorf("Batch failure result stored: %s", resultMessage);
    }
}
