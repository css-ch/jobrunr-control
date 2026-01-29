package ch.css.jobrunr.control.jobs.externaldata;

import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * Request for processing a single batch item in the external data processing job.
 * <p>
 * Each batch item represents a chunk of records to process from the source to the target system.
 */
public record ExternalDataBatchItemRequest(
        int childId,
        String stringExternalParameter,
        Boolean simulateErrors
) implements JobRequest {

    @Override
    public Class<ExternalDataBatchItemProcessor> getJobRequestHandler() {
        return ExternalDataBatchItemProcessor.class;
    }

    /**
     * Overrides {@code toString()} to provide a suitable job name in the JobRunr Dashboard.
     */
    @Override
    public String toString() {
        return String.format("Child %d (%s)", childId, stringExternalParameter);
    }
}
