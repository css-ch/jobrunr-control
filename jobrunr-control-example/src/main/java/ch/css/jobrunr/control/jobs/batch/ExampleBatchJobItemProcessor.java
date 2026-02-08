package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.domain.exceptions.JobProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@ApplicationScoped
@SuppressWarnings("java:S1192") // "attempted" literal duplication is acceptable for metadata key clarity
public class ExampleBatchJobItemProcessor implements JobRequestHandler<ExampleBatchJobItemProcessorRequest> {

    /**
     * Processes a single chunk/item from the batch job.
     * Simulates processing work with configurable error simulation for testing manual re-run behavior.
     * When simulateErrors is true:
     * - First run: All odd chunk IDs will fail
     * - Subsequent runs: All jobs succeed (demonstrating that re-running from dashboard fixes transient issues)
     *
     * @param request the batch item request containing chunk ID, chunk size, and error simulation flag
     * @throws JobProcessingException if processing fails or is interrupted, or if a simulated error occurs
     */
    @Override
    @Transactional
    @Job(name = "Processing example batch chunkId: %0", retries = 0)
    public void run(ExampleBatchJobItemProcessorRequest request) {
        jobContext().logger().info(String.format("Processing chunkId: %s", request.chunkId()));

        // Simulate transient errors: fail on first attempt, succeed on re-run
        // Check if this chunk has been attempted before using metadata
        boolean isFirstAttempt = !jobContext().getMetadata().containsKey("attempted");

        if (request.simulateErrors() && isFirstAttempt && Math.abs(request.chunkId() % 2) == 1) {
            // Mark as attempted for next time
            jobContext().saveMetadata("attempted", true);
            jobContext().logger().error("Simulated transient error for chunk ID: " + request.chunkId() + " (will succeed when batch is re-run from dashboard)");
            throw new JobProcessingException("Simulated transient error for chunk ID: " + request.chunkId());
        }

        // Mark as attempted even on success path
        jobContext().saveMetadata("attempted", true);

        for (int i = 0; i < request.chunkSize(); i++) {
            jobContext().logger().info(String.format("\tProcessing item in chunk: %s", i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobProcessingException("Batch item processing was interrupted", e);
            }
        }
        jobContext().logger().info("Processing done.");
    }
}
