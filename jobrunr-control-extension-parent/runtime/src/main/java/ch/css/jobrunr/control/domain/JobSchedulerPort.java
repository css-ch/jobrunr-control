package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port interface for job scheduling (Hexagonal Architecture).
 * Defines all operations for scheduling and managing jobs.
 */
public interface JobSchedulerPort {

    /**
     * Schedules a job with the given parameters at a specific time.
     *
     * @param jobDefinition Job definition
     * @param parameters    Parameter map for job execution
     * @param scheduledAt   Scheduled execution time
     * @return UUID of the scheduled job
     */
    UUID scheduleJob(JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt);

    /**
     * Schedules a job with additional labels.
     *
     * @param jobDefinition     Job definition
     * @param jobName           Job name
     * @param parameters        Parameter map for job execution
     * @param isExternalTrigger Whether job is externally triggered
     * @param scheduledAt       Scheduled execution time
     * @param additionalLabels  Additional labels for the job
     * @return UUID of the scheduled job
     */
    UUID scheduleJob(JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt, List<String> additionalLabels);

    /**
     * Updates a scheduled job directly (without deleting and recreating it).
     *
     * @param jobId       ID of the job to update
     * @param jobName     New job name
     * @param parameters  New parameters
     * @param scheduledAt New scheduled execution time
     */
    void updateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt);

    /**
     * Updates a scheduled job with additional labels.
     *
     * @param jobId             ID of the job to update
     * @param jobDefinition     Job definition
     * @param jobName           New job name
     * @param parameters        New parameters
     * @param isExternalTrigger Whether job is externally triggered
     * @param scheduledAt       New scheduled execution time
     * @param additionalLabels  Additional labels for the job
     */
    void updateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt, List<String> additionalLabels);

    /**
     * Deletes a scheduled job.
     *
     * @param jobId ID of the job to delete
     */
    void deleteScheduledJob(UUID jobId);

    /**
     * Returns all scheduled jobs.
     *
     * @return List of all scheduled jobs
     */
    List<ScheduledJobInfo> getScheduledJobs();

    /**
     * Returns a scheduled job by its ID.
     *
     * @param jobId Job ID
     * @return ScheduledJobInfo or null if not found
     */
    ScheduledJobInfo getScheduledJobById(UUID jobId);

    /**
     * Executes a scheduled job immediately.
     *
     * @param jobId ID of the job to execute
     */
    void executeJobNow(UUID jobId);

    /**
     * Executes a scheduled job immediately with metadata.
     *
     * @param jobId    ID of the job to execute
     * @param metadata Additional metadata
     */
    void executeJobNow(UUID jobId, Map<String, Object> metadata);
}
