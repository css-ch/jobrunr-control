package ch.css.jobrunr.control.jobs.batch;

public enum ExampleBatchJobProcessScenario {
    /**
     * Everything is fine, all chunk jobs succeed
     */
    JOB_SUCCESS,
    /**
     * Parameters are invalid, for example when Template does not match an updated JobType anymore. 
     */
    INVALID_PARAMETERS,
    /**
     * One or more chunk jobs fail, but the parent job is configured to continue processing other chunk jobs.
     */
    CHUNK_WARNING,
    /**
     * A chunk job fails due to business logic errors.
     */
    CHUNK_BUSINESS_ERROR,
    /**
     * A chunk job fails due to technical issues.
     */ 
    CHUNK_TECHNICAL_ERROR,
    /**
     * All chunk jobs succeed, but the parent job fails due to a post-processing error. 
     */
    POSTJOB_ERROR
}
