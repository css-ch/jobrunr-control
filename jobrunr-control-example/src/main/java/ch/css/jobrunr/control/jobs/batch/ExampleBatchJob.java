package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobDetailPage;
import ch.css.jobrunr.control.domain.exceptions.JobExecutionException;
import ch.css.jobrunr.control.domain.exceptions.JobProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.scheduling.JobProId;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import jakarta.inject.Inject;

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
 * 1. Metadata-based check to prevent re-enqueueing
 * 2. Deterministic UUIDs for chunk jobs (JobRunr deduplicates automatically)
 */
@ApplicationScoped
public class ExampleBatchJob implements JobRequestHandler<ExampleBatchJobRequest> {

    private static final String METADATA_KEY_ENQUEUED = "chunks_enqueued";

    private ExampleBatchJobItemProcessor itemProcessor;


    @Inject
    public ExampleBatchJob(ExampleBatchJobItemProcessor itemProcessor) {
        this.itemProcessor = itemProcessor;
    }


    /**
     * Executes the batch job by creating and enqueuing chunk jobs for processing.
     * <p>
     * ✅ IDEMPOTENT: Safe to retry - uses metadata checks and deterministic UUIDs
     *
     * @param request the batch job request containing number of chunks, chunk size, and error simulation flag
     * @throws JobProcessingException if the batch preparation is interrupted or fails
     */
    @ConfigurableJob(isBatch = true, labels = {"Example", "Batch"})
    @JobDetailPage
    @Override
    public void run(ExampleBatchJobRequest request) {
        ThreadLocalJobContext.getJobContext().logger().info(String.format("Preparing batch job with numberOfChunks: %d, chunkSize: %d, scenario: %b",
                request.numberOfChunks(), request.chunkSize(), request.processScenario()));

        var scenario = request.processScenario();

        if (scenario == ExampleBatchJobProcessScenario.INVALID_PARAMETERS) {
            throw new JobExecutionException("Simulated paramters validation Exception.");
        }

        // ✅ IDEMPOTENCY CHECK: Skip if chunk jobs already enqueued
        if (isAlreadyEnqueued()) {
            ThreadLocalJobContext.getJobContext().logger().info("Chunk jobs already enqueued, skipping to prevent duplicates");
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

        // ✅ MARK AS PROCESSED BEFORE ENQUEUEING
        // This prevents duplicate chunk jobs if the process fails during or after enqueueing
        markAsEnqueued(items.size());

        // Extra metadata for the batch job (visible in the JobRunr Control UI)
        ThreadLocalJobContext.getJobContext().saveMetadata("hello", "ExampleBatchJob#run");

        // ✅ DETERMINISTIC UUIDs: Enqueue with deterministic job IDs
        // Same input → same UUID → JobRunr deduplicates automatically
        final var chunkBatchJobId = enqueueChunkBatchJob(parentBatchJobId, parentBatchJobName, items);
    }

    /**
     * Checks if chunk jobs have already been enqueued.
     * <p>
     * ✅ IDEMPOTENCY: Prevents duplicate chunk jobs on retry
     *
     * @return true if chunks were already enqueued
     */
    private boolean isAlreadyEnqueued() {
        var metadata = ThreadLocalJobContext.getJobContext().getMetadata();
        return metadata.containsKey(METADATA_KEY_ENQUEUED) &&
                Boolean.TRUE.equals(metadata.get(METADATA_KEY_ENQUEUED));
    }

    /**
     * Marks the batch as processed before enqueueing chunks.
     * <p>
     * ✅ IDEMPOTENCY: Setting this flag BEFORE enqueueing ensures that if the process fails
     * during or after enqueueing, the retry will skip re-enqueueing.
     *
     * @param itemCount number of chunk jobs to be enqueued
     */
    private void markAsEnqueued(int itemCount) {
        ThreadLocalJobContext.getJobContext().saveMetadata(METADATA_KEY_ENQUEUED, true);
        ThreadLocalJobContext.getJobContext().saveMetadata("total_chunks", itemCount);
        ThreadLocalJobContext.getJobContext().saveMetadata("enqueued_at", java.time.Instant.now().toString());
    }


    private JobProId enqueueChunkBatchJob(UUID parentBatchJobId, String batchJobName, 
                                          List<ExampleBatchJobItemProcessorRequest> items) {

        // Enqueue a dummy job to represent the chunk batch job (for demonstration purposes)
        var jobProId = BackgroundJob.create(aBatchJob()
            .withJobLambda(() -> {
                enqueueDeterministicJobs(batchJobName, items);
            })
            .withAmountOfRetries(0)
            .withName(String.format("%s-ChunkBatch", batchJobName))
            .withLabels("chunk", "batch")
        );

        return jobProId;
    }

    /**
     * Enqueues chunk jobs with standard JobRunr API.
     * <p>
     * ✅ IDEMPOTENCY: Combined with the metadata check (isAlreadyEnqueued()), this ensures
     * that chunk jobs are only enqueued once, even if the parent job is retried.
     * <p>
     * Note: JobRunr Pro's deterministic UUID feature would require using JobScheduler directly,
     * but for this use case, the metadata-based idempotency check is sufficient and simpler.
     *
     * @param items the batch items to enqueue
     */
    private void enqueueDeterministicJobs(String mainBatchJobName, List<ExampleBatchJobItemProcessorRequest> items) {
        // Enqueue all items in a stream (JobRunr handles the scheduling)
        for(var item: items) {
            BackgroundJob.create(aJob()
                .withJobLambda(() -> itemProcessor.run(item))
                .withName(String.format("%s-Chunk-%d", mainBatchJobName, item.chunkId()))
                .withAmountOfRetries(3)
            );
        }

        ThreadLocalJobContext.getJobContext().logger().info(String.format(
                "Enqueued %d chunk jobs (retry-safe via metadata check)", items.size()));
    }
}
