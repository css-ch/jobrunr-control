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
     * Finds a job execution by ID.
     *
     * @param jobId Job ID
     * @return Optional with job execution information, if found
     */
    Optional<JobExecutionInfo> getJobExecutionById(UUID jobId);

    /**
     * Creates a deep link to the JobRunr Dashboard for a specific job.
     *
     * @param jobId Job ID
     * @return URL to the JobRunr Dashboard
     */
    String getDashboardDeepLink(UUID jobId);
}

