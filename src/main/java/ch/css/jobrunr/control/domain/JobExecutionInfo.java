package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.*;

/**
 * Represents information about a job execution.
 * Contains job ID, name (user-defined), type (SimpleClassName), status, timestamps, and optional batch progress.
 */
public class JobExecutionInfo {

    private final UUID jobId;
    private final String jobName; // User-defined name of the job instance
    private final String jobType; // SimpleClassName of the job class
    private final JobStatus status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final BatchProgress batchProgress;
    private final Map<String, Object> parameters;

    public JobExecutionInfo(UUID jobId, String jobName, String jobType, JobStatus status,
                            Instant startedAt, Instant finishedAt, BatchProgress batchProgress,
                            Map<String, Object> parameters) {
        this.jobId = Objects.requireNonNull(jobId, "Job ID must not be null");
        this.jobName = Objects.requireNonNull(jobName, "Job Name must not be null");
        this.jobType = Objects.requireNonNull(jobType, "Job Type must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.batchProgress = batchProgress;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

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

    public boolean isBatchJob() {
        return batchProgress != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobExecutionInfo that = (JobExecutionInfo) o;
        return Objects.equals(jobId, that.jobId) &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(jobType, that.jobType) &&
                status == that.status &&
                Objects.equals(startedAt, that.startedAt) &&
                Objects.equals(finishedAt, that.finishedAt) &&
                Objects.equals(batchProgress, that.batchProgress) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, jobName, jobType, status, startedAt, finishedAt, batchProgress, parameters);
    }

    @Override
    public String toString() {
        return "JobExecutionInfo{" +
                "jobId=" + jobId +
                ", jobName='" + jobName + '\'' +
                ", jobType='" + jobType + '\'' +
                ", status=" + status +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", batchProgress=" + batchProgress +
                ", parameters=" + parameters +
                '}';
    }
}

