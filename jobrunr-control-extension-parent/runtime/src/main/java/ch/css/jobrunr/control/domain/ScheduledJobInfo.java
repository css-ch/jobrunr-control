package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.*;

/**
 * Represents a scheduled job with metadata.
 * Contains job ID, name (user-defined), type (SimpleClassName), schedule, and parameters.
 */
public class ScheduledJobInfo {

    private final UUID jobId;
    private final String jobName; // User-defined name of the job instance
    private final String jobType; // SimpleClassName of the job class
    private final Instant scheduledAt;
    private final Map<String, Object> parameters;
    private final boolean isExternallyTriggerable;
    private final List<String> labels;

    public ScheduledJobInfo(UUID jobId, String jobName, String jobType, Instant scheduledAt,
                            Map<String, Object> parameters, boolean isExternallyTriggerable) {
        this(jobId, jobName, jobType, scheduledAt, parameters, isExternallyTriggerable, Collections.emptyList());
    }

    public ScheduledJobInfo(UUID jobId, String jobName, String jobType, Instant scheduledAt,
                            Map<String, Object> parameters, boolean isExternallyTriggerable, List<String> labels) {
        this.jobId = Objects.requireNonNull(jobId, "Job ID must not be null");
        this.jobName = Objects.requireNonNull(jobName, "Job Name must not be null");
        this.jobType = Objects.requireNonNull(jobType, "Job Type must not be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "Scheduled at must not be null");
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.isExternallyTriggerable = isExternallyTriggerable;
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
    }

    public int getParameterCount() {
        return parameters.size();
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

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public boolean isExternallyTriggerable() {
        return isExternallyTriggerable;
    }

    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledJobInfo that = (ScheduledJobInfo) o;
        return isExternallyTriggerable == that.isExternallyTriggerable &&
                Objects.equals(jobId, that.jobId) &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(jobType, that.jobType) &&
                Objects.equals(scheduledAt, that.scheduledAt) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, jobName, jobType, scheduledAt, parameters, isExternallyTriggerable, labels);
    }

    @Override
    public String toString() {
        return "ScheduledJobInfo{" +
                "jobId=" + jobId +
                ", jobName='" + jobName + '\'' +
                ", jobType='" + jobType + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", parameters=" + parameters +
                ", isExternallyTriggerable=" + isExternallyTriggerable +
                ", labels=" + labels +
                '}';
    }
}

