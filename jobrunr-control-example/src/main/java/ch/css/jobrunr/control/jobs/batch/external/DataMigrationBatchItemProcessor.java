package ch.css.jobrunr.control.jobs.batch.external;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Random;

/**
 * Processes a single batch item in the data migration.
 * <p>
 * This simulates migrating a batch of records from the source system to the target system.
 * Each record in the batch is processed sequentially with a small delay to simulate actual work.
 */
@ApplicationScoped
public class DataMigrationBatchItemProcessor implements JobRequestHandler<DataMigrationBatchItemRequest> {

    private static final Random random = new Random();

    @Override
    @Transactional
    @Job(name = "Data Migration: %0", retries = 2)
    public void run(DataMigrationBatchItemRequest request) throws Exception {
        jobContext().logger().info(String.format(
                "Processing batch %d: migrating %d records from %s to %s (date: %s)",
                request.batchId(),
                request.batchSize(),
                request.sourceName(),
                request.targetName(),
                request.migrationDate()));

        int successCount = 0;
        int errorCount = 0;

        // Process each record in the batch
        for (int i = 0; i < request.batchSize(); i++) {
            int recordNumber = (request.batchId() - 1) * request.batchSize() + i + 1;

            try {
                // Simulate reading from source
                Thread.sleep(50);

                // Log every 10th record to avoid log spam
                if (i % 10 == 0) {
                    jobContext().logger().info(String.format(
                            "Migrating record #%d from %s to %s",
                            recordNumber,
                            request.sourceName(),
                            request.targetName()));
                }

                // Simulate writing to target
                Thread.sleep(50);

                // Simulate occasional errors if enabled
                if (request.simulateErrors() && random.nextDouble() < 0.01) {
                    throw new Exception("Simulated migration error for record " + recordNumber);
                }

                successCount++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Migration interrupted", e);
            } catch (Exception e) {
                errorCount++;
                jobContext().logger().error(String.format(
                        "Error migrating record #%d: %s",
                        recordNumber,
                        e.getMessage()));

                // If too many errors, fail the batch
                if (errorCount > request.batchSize() * 0.1) {
                    throw new Exception(String.format(
                            "Too many errors in batch %d: %d/%d failed",
                            request.batchId(),
                            errorCount,
                            request.batchSize()));
                }
            }
        }

        // Save batch processing statistics
        jobContext().saveMetadata("successCount", successCount);
        jobContext().saveMetadata("errorCount", errorCount);
        jobContext().saveMetadata("totalRecords", request.batchSize());

        jobContext().logger().info(String.format(
                "Batch %d completed: %d successful, %d errors",
                request.batchId(),
                successCount,
                errorCount));
    }
}
