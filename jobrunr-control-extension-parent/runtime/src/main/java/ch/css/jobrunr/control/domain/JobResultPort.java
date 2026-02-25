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
}

