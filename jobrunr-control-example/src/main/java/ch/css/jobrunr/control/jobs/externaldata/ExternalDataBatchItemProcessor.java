package ch.css.jobrunr.control.jobs.externaldata;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Random;

/**
 * Processes a single child job in the external data batch processing.
 * <p>
 * This demonstrates processing individual child jobs with parameters from the parent batch job.
 * Each child job receives its ID and string parameter for processing.
 */
@ApplicationScoped
public class ExternalDataBatchItemProcessor implements JobRequestHandler<ExternalDataBatchItemRequest> {

    private static final Random random = new Random();

    @Override
    @Transactional
    @Job(name = "External Data Processing: %0", retries = 2)
    public void run(ExternalDataBatchItemRequest request) throws Exception {
        jobContext().logger().info(String.format(
                "Processing child job %d with parameter: %s",
                request.childId(),
                request.stringExternalParameter()));

        try {
            // Simulate data processing work
            Thread.sleep(100);

            jobContext().logger().info(String.format(
                    "Child job %d: Processing data with parameter '%s'",
                    request.childId(),
                    request.stringExternalParameter()));

            // Simulate additional work
            Thread.sleep(100);

            // Simulate occasional errors if enabled
            if (request.simulateErrors() && random.nextDouble() < 0.1) {
                throw new Exception("Simulated processing error for child job " + request.childId());
            }

            // Save processing metadata
            jobContext().saveMetadata("childId", request.childId());
            jobContext().saveMetadata("stringParameter", request.stringExternalParameter());
            jobContext().saveMetadata("status", "success");

            jobContext().logger().info(String.format(
                    "Child job %d completed successfully",
                    request.childId()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Processing interrupted", e);
        } catch (Exception e) {
            jobContext().logger().error(String.format(
                    "Error in child job %d: %s",
                    request.childId(),
                    e.getMessage()));
            jobContext().saveMetadata("status", "failed");
            throw e;
        }
    }
}
