package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.*;

/**
 * Represents information about a job execution.
 * Contains job ID, name (user-defined), type (SimpleClassName), status, timestamps, and optional batch progress.
 */
public record JobExecutionInfo(
        UUID jobId,
        String jobName, // User-defined name of the job instance
        String jobType, // SimpleClassName of the job class
        JobStatus status,
        Instant startedAt,
        Instant finishedAt,
        BatchProgress batchProgress,
        Map<String, Object> parameters,
        Map<String, Object> metadata
) {

    public JobExecutionInfo {
        Objects.requireNonNull(jobId, "Job ID must not be null");
        Objects.requireNonNull(jobName, "Job Name must not be null");
        Objects.requireNonNull(jobType, "Job Type must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // JavaBean-style getters for backward compatibility
    public UUID getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobType() {
        return jobType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Optional<Instant> getFinishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<BatchProgress> getBatchProgress() {
        return Optional.ofNullable(batchProgress);
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @SuppressWarnings("unused") // Used in templates
    public boolean isBatchJob() {
        return batchProgress != null;
    }
}

