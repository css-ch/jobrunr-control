package ch.css.jobrunr.control.domain;

/**
 * Port for storing job results in a job's metadata.
 * <p>
 * Results are stored with a result code (integer) and a result message (string).
 * The implementation automatically handles storing results in parent jobs when running
 * in continuation jobs (success/failure handlers).
 */
public interface JobResultPort {


    /**
     * Stores both a result code and a result message in the appropriate job's metadata.
     * <p>
     * If running in a continuation job (success/failure handler) with an awaited parent job,
     * the result will be stored in the parent job. Otherwise, it will be stored in the current job.
     * <p>
     * This automatic behavior ensures that batch job results appear on the parent job that the user
     * created, not on internal handler jobs.
     *
     * @param resultCode the result code (0 for success, non-zero for specific outcomes)
     * @param result     the result message
     */
    void storeResult(int resultCode, String result);

    /**
     * Stores a Batch-Status in the Metadata of the Job. This Batch-Status will be shown in the
     * Execution-History and can be used to track the progress of a Batch-Job. It will not override the actual Job-Status, but can be used to provide more detailed information about the progress of a Batch-Job.
     * On the Execution-History the following Status-Labels will be colored:
     * - PROCESSING
     * - ENQUEUED
     * - PROCESSED
     * - POSTPROCESSING
     * - SUCCEEDED
     * - WARNING
     * - FAILED
     * @param overrideBatchStatus the batch status to be stored
     */
    void overrideBatchStatus(String overrideBatchStatus);

    /**
     * Same as overrideBatchStatus, but runs in a new transaction. This is useful when the Batch-Status needs to be overridden in a Job that is running in the same transaction as the Main-Batch-Job.
     * In this case, the Batch-Status will not be updated in the Execution-History until the Main-Batch-Job has finished.
     * By running in a new transaction, the Batch-Status can be updated immediately and will be visible in the Execution-History even while the Main-Batch-Job is still running.
     * @param overrideBatchStatus the batch status to be stored
     */
    void overrideBatchStatusTxNew(String overrideBatchStatus);

    /**
     * Removes the overridedStatus. That means the Batch-Status of the Main-Batch-Job will bee shown at the Execution-History.
     */
    void resetBatchStatus();
}
