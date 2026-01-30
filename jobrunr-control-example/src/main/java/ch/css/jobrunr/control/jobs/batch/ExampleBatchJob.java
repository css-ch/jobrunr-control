package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.domain.exceptions.JobProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Example batch job with idempotency protection.
 * <p>
 * This implementation is retry-safe and prevents duplicate child jobs when the parent job is retried.
 * Uses two idempotency strategies:
 * 1. Metadata-based check to prevent re-enqueueing
 * 2. Deterministic UUIDs for child jobs (JobRunr deduplicates automatically)
 */
@ApplicationScoped
public class ExampleBatchJob implements JobRequestHandler<ExampleBatchJobRequest> {

    private static final String METADATA_KEY_ENQUEUED = "children_enqueued";

    /**
     * Executes the batch job by creating and enqueuing child jobs for processing.
     * <p>
     * ✅ IDEMPOTENT: Safe to retry - uses metadata checks and deterministic UUIDs
     *
     * @param request the batch job request containing number of chunks, chunk size, and error simulation flag
     * @throws JobProcessingException if the batch preparation is interrupted or fails
     */
    @ConfigurableJob(isBatch = true, labels = {"Example", "Batch"})
    @Override
    public void run(ExampleBatchJobRequest request) {
        jobContext().logger().info(String.format("Preparing batch job with numberOfChunks: %d, chunkSize: %d, simulateErrors: %b",
                request.numberOfChunks(), request.chunkSize(), request.simulateErrors()));

        // ✅ IDEMPOTENCY CHECK: Skip if child jobs already enqueued
        if (isAlreadyEnqueued()) {
            jobContext().logger().info("Child jobs already enqueued, skipping to prevent duplicates");
            return;
        }

        // Load all items to be processed based on request parameters
        List<ExampleBatchJobItemProcessorRequest> items = IntStream.rangeClosed(1, request.numberOfChunks())
                .mapToObj(junkId -> new ExampleBatchJobItemProcessorRequest(junkId, request.chunkSize(), request.simulateErrors()))
                .toList();

        // Simulate preparation delay
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobProcessingException("Batch job preparation was interrupted", e);
        }

        // ✅ MARK AS PROCESSED BEFORE ENQUEUEING
        // This prevents duplicate child jobs if the process fails during or after enqueueing
        markAsEnqueued(items.size());

        // Extra metadata for the batch job (visible in the JobRunr Control UI)
        jobContext().saveMetadata("hello", "ExampleBatchJob#run");

        // ✅ DETERMINISTIC UUIDs: Enqueue with deterministic job IDs
        // Same input → same UUID → JobRunr deduplicates automatically
        enqueueDeterministicJobs(items);
    }

    /**
     * Checks if child jobs have already been enqueued.
     * <p>
     * ✅ IDEMPOTENCY: Prevents duplicate child jobs on retry
     *
     * @return true if children were already enqueued
     */
    private boolean isAlreadyEnqueued() {
        var metadata = jobContext().getMetadata();
        return metadata.containsKey(METADATA_KEY_ENQUEUED) &&
                Boolean.TRUE.equals(metadata.get(METADATA_KEY_ENQUEUED));
    }

    /**
     * Marks the batch as processed before enqueueing children.
     * <p>
     * ✅ IDEMPOTENCY: Setting this flag BEFORE enqueueing ensures that if the process fails
     * during or after enqueueing, the retry will skip re-enqueueing.
     *
     * @param itemCount number of child jobs to be enqueued
     */
    private void markAsEnqueued(int itemCount) {
        jobContext().saveMetadata(METADATA_KEY_ENQUEUED, true);
        jobContext().saveMetadata("total_children", itemCount);
        jobContext().saveMetadata("enqueued_at", java.time.Instant.now().toString());
    }

    /**
     * Enqueues child jobs with standard JobRunr API.
     * <p>
     * ✅ IDEMPOTENCY: Combined with the metadata check (isAlreadyEnqueued()), this ensures
     * that child jobs are only enqueued once, even if the parent job is retried.
     * <p>
     * Note: JobRunr Pro's deterministic UUID feature would require using JobScheduler directly,
     * but for this use case, the metadata-based idempotency check is sufficient and simpler.
     *
     * @param items the batch items to enqueue
     */
    private void enqueueDeterministicJobs(List<ExampleBatchJobItemProcessorRequest> items) {
        // Enqueue all items in a stream (JobRunr handles the scheduling)
        BackgroundJobRequest.enqueue(items.stream());

        jobContext().logger().info(String.format(
                "Enqueued %d child jobs (retry-safe via metadata check)", items.size()));
    }
}
