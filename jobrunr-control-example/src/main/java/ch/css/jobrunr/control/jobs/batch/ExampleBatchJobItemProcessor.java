package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.domain.exceptions.JobProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Random;

@ApplicationScoped
public class ExampleBatchJobItemProcessor implements JobRequestHandler<ExampleBatchJobItemProcessorRequest> {

    private static final Random random = new Random();

    /**
     * Processes a single chunk/item from the batch job.
     * Simulates processing work with configurable error simulation for testing retry mechanisms.
     *
     * @param request the batch item request containing chunk ID, chunk size, and error simulation flag
     * @throws JobProcessingException if processing fails or is interrupted, or if a simulated error occurs
     */
    @Override
    @Transactional
    @Job(name = "Processing example batch chunkId: %0", retries = 0)
    public void run(ExampleBatchJobItemProcessorRequest request) {
        jobContext().logger().info(String.format("Processing chunkId: %s", request.chunkId()));
        for (int i = 0; i < request.chunkSize(); i++) {
            jobContext().logger().info(String.format("\tProcessing item in junk: %s", i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobProcessingException("Batch item processing was interrupted", e);
            }
            if (request.simulateErrors()) {
                if (random.nextDouble() < (0.02 / request.chunkSize())) {
                    jobContext().logger().error("Simulated error processing item: " + request.chunkId());
                    throw new JobProcessingException("Simulated error processing item: " + request.chunkId());
                }
            }
        }
        jobContext().logger().info("Processing done.");
    }
}
