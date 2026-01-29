package ch.css.jobrunr.control.jobs.batch.external;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDate;

/**
 * Request for processing a single batch item in the data migration.
 * <p>
 * Each batch item represents a chunk of records to migrate from the source to the target system.
 */
public record DataMigrationBatchItemRequest(
        int batchId,
        int batchSize,
        String sourceName,
        String targetName,
        LocalDate migrationDate,
        Boolean simulateErrors
) implements JobRequest {

    @Override
    public Class<DataMigrationBatchItemProcessor> getJobRequestHandler() {
        return DataMigrationBatchItemProcessor.class;
    }

    /**
     * Overrides {@code toString()} to provide a suitable job name in the JobRunr Dashboard.
     */
    @Override
    public String toString() {
        return String.format("Batch %d (%s->%s)", batchId, sourceName, targetName);
    }
}
