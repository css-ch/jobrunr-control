package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.*;

/**
 * Represents a scheduled job with metadata.
 * Contains job ID, name (user-defined), type (SimpleClassName), schedule, and parameters.
 *
 * @param jobId                   the unique job identifier
 * @param jobName                 user-defined name of the job instance
 * @param jobDefinition           the definition of the job
 * @param scheduledAt             when the job is scheduled to run
 * @param parameters              job parameters (immutable copy)
 * @param isExternallyTriggerable whether the job is externally triggerable
 * @param labels                  job labels (immutable copy)
 */
public record ScheduledJobInfo(
        UUID jobId,
        String jobName,
        JobDefinition jobDefinition,
        Instant scheduledAt,
        Map<String, Object> parameters,
        boolean isExternallyTriggerable,
        List<String> labels
) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public ScheduledJobInfo {
        Objects.requireNonNull(jobId, "Job ID must not be null");
        jobName = Objects.requireNonNull(jobName, "Job Name must not be null");
        Objects.requireNonNull(jobDefinition, "Job definition must not be null");
        Objects.requireNonNull(scheduledAt, "Scheduled at must not be null");
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        labels = labels != null ? List.copyOf(labels) : List.of();
    }

    /**
     * Convenience constructor without labels.
     */
    public ScheduledJobInfo(UUID jobId, String jobName, JobDefinition jobDefinition, Instant scheduledAt,
                            Map<String, Object> parameters, boolean isExternallyTriggerable) {
        this(jobId, jobName, jobDefinition, scheduledAt, parameters, isExternallyTriggerable, List.of());
    }

    // JavaBean-style getters for compatibility with existing code

    public UUID getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public JobDefinition getJobDefinition() {
        return jobDefinition;
    }

    /**
     * Returns the job type (simple class name).
     * Delegates to jobDefinition.jobType().
     */
    public String getJobType() {
        return jobDefinition.jobType();
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public List<String> getLabels() {
        return labels;
    }

    /**
     * Checks if this job is a template job.
     *
     * @return true if this job has the "template" label
     */
    public boolean isTemplate() {
        return labels.contains("template");
    }

    /**
     * Checks if this job uses external parameter storage.
     *
     * @return true if parameters are stored externally
     */
    public boolean hasExternalParameters() {
        return parameters.containsKey("__parameterSetId");
    }

    /**
     * Gets the parameter set ID if using external storage.
     *
     * @return the parameter set ID, or empty if inline storage
     */
    public Optional<UUID> getParameterSetId() {
        Object value = parameters.get("__parameterSetId");
        if (value instanceof String str) {
            return Optional.of(UUID.fromString(str));
        }
        return Optional.empty();
    }
}

