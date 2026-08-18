package ch.css.jobrunr.control.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for job execution queries.
 * Enables retrieval of information about job executions.
 */
public interface JobExecutionPort {

    /**
     * Returns a paginated list of job executions.
     *
     * @return List of job execution information
     */
    List<JobExecutionInfo> getJobExecutions();

    /**
     * Returns executions for one job type, or all executions when the type is blank.
     *
     * @param jobType optional stable job type identifier
     * @return List of matching job execution information
     */
    default List<JobExecutionInfo> getJobExecutions(String jobType) {
        return getJobExecutions();
    }

    /**
     * Finds a job execution by ID.
     *
     * @param jobId Job ID
     * @return Optional with job execution information, if found
     */
    Optional<JobExecutionInfo> getJobExecutionById(UUID jobId);

    /**
     * Finds a job execution by ID. And checks the state of the entire job chain.
     *
     * @param jobId Job ID
     * @return Optional with job execution information, if found
     */
    Optional<JobExecutionInfo> getJobChainExecutionById(UUID jobId);

}
