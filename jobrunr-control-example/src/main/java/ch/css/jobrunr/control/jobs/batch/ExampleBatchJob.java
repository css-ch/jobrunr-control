package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobDetailPage;
import ch.css.jobrunr.control.domain.exceptions.JobExecutionException;
import ch.css.jobrunr.control.domain.exceptions.JobProcessingException;
import ch.css.jobrunr.control.jobs.batch.postprocess.ExampleBatchFailureRequest;
import ch.css.jobrunr.control.jobs.batch.postprocess.ExampleBatchSuccessRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.scheduling.JobProId;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.jobrunr.scheduling.JobBuilder.aBatchJob;
import static org.jobrunr.scheduling.JobBuilder.aJob;

/**
 * Example batch job with idempotency protection.
 * <p>
 * This implementation is retry-safe and prevents duplicate chunk jobs when the parent job is retried.
 * Uses two idempotency strategies:
 * 1. A durable JobRunr step that is marked only after the complete workflow was configured
 * 2. Deterministic UUIDs for the nested batch, post-processing jobs and chunk jobs
 */
@ApplicationScoped
public class ExampleBatchJob implements JobRequestHandler<ExampleBatchJobRequest> {

    private static final String ENQUEUE_WORKFLOW_STEP = "enqueue-workflow";
    private static final String METADATA_KEY_ENQUEUED = "chunks_enqueued";

    /**
     * Executes the batch job by creating and enqueuing chunk jobs for processing.
     * <p>
     * ✅ IDEMPOTENT: Safe to retry - uses a durable execution step and deterministic UUIDs
     *
     * @param request the batch job request containing number of chunks, chunk size, and error simulation flag
     * @throws JobProcessingException if the batch preparation is interrupted or fails
     */
    @ConfigurableJob(isBatch = true, labels = {"Example", "Batch"})
    @JobDetailPage
    @Override
    public void run(ExampleBatchJobRequest request) {
        ThreadLocalJobContext.getJobContext().logger().info(String.format("Preparing batch job with numberOfChunks: %d, chunkSize: %d, scenario: %s",
                request.numberOfChunks(), request.chunkSize(), request.processScenario()));

        var scenario = request.processScenario();

        if (scenario == ExampleBatchJobProcessScenario.INVALID_PARAMETERS) {
            throw new JobExecutionException("Simulated paramters validation Exception.");
        }

        var jobContext = ThreadLocalJobContext.getJobContext();
        if (jobContext.hasCompletedStep(ENQUEUE_WORKFLOW_STEP)) {
            jobContext.logger().info("Batch workflow already enqueued, skipping completed durable step");
            return;
        }

        // Get the parent batch job ID to pass to chunk jobs
        var parentBatchJobId = ThreadLocalJobContext.getJobContext().getJobId();
        var parentBatchJobName = ThreadLocalJobContext.getJobContext().getJobName();

        // Load all items to be processed based on request parameters
        List<ExampleBatchJobItemProcessorRequest> items = IntStream.rangeClosed(1, request.numberOfChunks())
                .mapToObj(junkId -> new ExampleBatchJobItemProcessorRequest(junkId, request.chunkSize(), request.processScenario(), parentBatchJobId))
                .toList();

        // Simulate preparation delay
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobProcessingException("Batch job preparation was interrupted", e);
        }

        // Extra metadata for the batch job (visible in the JobRunr Control UI)
        jobContext.saveMetadata("hello", "ExampleBatchJob#run");

        // The step marker is persisted only after all jobs were configured. If this block is
        // retried after a partial failure, deterministic IDs make already-created jobs no-ops.
        jobContext.runStepOnce(ENQUEUE_WORKFLOW_STEP, () -> {
            JobProId chunkBatchJobId = enqueueChunkBatchJob(parentBatchJobId, parentBatchJobName, items);
            enqueuePostProcessingJobs(parentBatchJobId, parentBatchJobName, scenario, chunkBatchJobId);
            markAsEnqueued(items.size());
        });
    }

    /**
     * Records diagnostic metadata after the complete workflow was configured.
     * <p>
     * @param itemCount number of chunk jobs to be enqueued
     */
    private void markAsEnqueued(int itemCount) {
        ThreadLocalJobContext.getJobContext().saveMetadata(METADATA_KEY_ENQUEUED, true);
        ThreadLocalJobContext.getJobContext().saveMetadata("total_chunks", itemCount);
        ThreadLocalJobContext.getJobContext().saveMetadata("enqueued_at", java.time.Instant.now().toString());
    }


    private JobProId enqueueChunkBatchJob(UUID parentBatchJobId, String batchJobName,
                                          List<ExampleBatchJobItemProcessorRequest> items) {
        return BackgroundJob.create(aBatchJob()
            .withId(deterministicJobId(parentBatchJobId, "chunk-batch"))
            .withJobLambda(() -> enqueueChunkJobs(batchJobName, items))
            .withAmountOfRetries(0)
            .withName(String.format("%s-ChunkBatch", batchJobName))
        );
    }

    private void enqueuePostProcessingJobs(UUID parentBatchJobId, String batchJobName,
                                           ExampleBatchJobProcessScenario scenario, JobProId chunkBatchJobId) {
        BackgroundJobRequest.create(aJob()
                .withId(deterministicJobId(parentBatchJobId, "post-success"))
                .runAfterSuccessOf(chunkBatchJobId)
                .withJobRequest(new ExampleBatchSuccessRequest(parentBatchJobId, scenario))
                .withAmountOfRetries(0)
                .withName(String.format("%s-PostSuccess", batchJobName)));

        BackgroundJobRequest.create(aJob()
                .withId(deterministicJobId(parentBatchJobId, "post-failure"))
                .runAfterFailureOf(chunkBatchJobId)
                .withJobRequest(new ExampleBatchFailureRequest(parentBatchJobId))
                .withAmountOfRetries(0)
                .withName(String.format("%s-PostFailure", batchJobName)));
    }

    /**
     * Enqueues chunk jobs with standard JobRunr API.
     * <p>
     * ✅ IDEMPOTENCY: Deterministic IDs ensure that retrying the durable enqueue step does not
     * create duplicates.
     * <p>
     * @param items the batch items to enqueue
     */
    public void enqueueChunkJobs(String mainBatchJobName, List<ExampleBatchJobItemProcessorRequest> items) {
        for (var item : items) {
            BackgroundJobRequest.create(aJob()
                .withId(deterministicJobId(item.parentBatchJobId(), "chunk-" + item.chunkId()))
                .withJobRequest(item)
                .withName(String.format("%s-Chunk-%d", mainBatchJobName, item.chunkId()))
                .withAmountOfRetries(3)
            );
        }

        ThreadLocalJobContext.getJobContext().logger().info(String.format(
                "Enqueued %d chunk jobs (retry-safe via durable step and deterministic IDs)", items.size()));
    }

    private static UUID deterministicJobId(UUID parentBatchJobId, String role) {
        return UUID.nameUUIDFromBytes((parentBatchJobId + ":" + role).getBytes(StandardCharsets.UTF_8));
    }
}
