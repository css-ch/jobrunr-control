package ch.css.jobrunr.control.jobs.batch.external;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Example batch job using external parameter storage.
 * <p>
 * This job demonstrates how to:
 * 1. Load parameters from external storage using the parameter set ID
 * 2. Process parameters and create batch items
 * 3. Enqueue background jobs for parallel processing
 * <p>
 * The job simulates a data migration process from a legacy system to a new system.
 */
@ApplicationScoped
public class DataMigrationBatchJob implements JobRequestHandler<DataMigrationBatchJobRequest> {

    @Inject
    ParameterStorageService parameterStorageService;

    @ConfigurableJob(isBatch = true, labels = {"DataMigration"})
    @Transactional
    @Override
    public void run(DataMigrationBatchJobRequest request) throws Exception {
        // Load parameters from external storage
        UUID parameterSetId = UUID.fromString(request.parameterSetId());
        ParameterSet parameterSet = parameterStorageService.findById(parameterSetId)
                .orElseThrow(() -> new IllegalStateException(
                        "Parameter set not found: " + parameterSetId));

        // Extract parameters
        String sourceName = (String) parameterSet.parameters().get("sourceName");
        String targetName = (String) parameterSet.parameters().get("targetName");
        Integer numberOfBatches = (Integer) parameterSet.parameters().get("numberOfBatches");
        Integer batchSize = (Integer) parameterSet.parameters().get("batchSize");
        Boolean simulateErrors = (Boolean) parameterSet.parameters().get("simulateErrors");
        String migrationDateStr = (String) parameterSet.parameters().get("migrationDate");
        LocalDate migrationDate = LocalDate.parse(migrationDateStr);

        jobContext().logger().info(String.format(
                "Starting data migration batch job: %s -> %s, batches: %d, batchSize: %d, date: %s, simulateErrors: %b",
                sourceName, targetName, numberOfBatches, batchSize, migrationDate, simulateErrors));

        // Create batch items
        List<DataMigrationBatchItemRequest> items = IntStream.rangeClosed(1, numberOfBatches)
                .mapToObj(batchId -> new DataMigrationBatchItemRequest(
                        batchId,
                        batchSize,
                        sourceName,
                        targetName,
                        migrationDate,
                        simulateErrors
                ))
                .toList();

        // Simulate preparation delay (e.g., connecting to source system)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Save metadata
        jobContext().saveMetadata("sourceName", sourceName);
        jobContext().saveMetadata("targetName", targetName);
        jobContext().saveMetadata("migrationDate", migrationDate.toString());
        jobContext().saveMetadata("totalBatches", numberOfBatches);
        jobContext().saveMetadata("totalRecords", numberOfBatches * batchSize);

        jobContext().logger().info(String.format(
                "Enqueueing %d batch items for processing...", items.size()));

        // Enqueue background jobs for each batch item
        BackgroundJobRequest.enqueue(items.stream());
    }
}
