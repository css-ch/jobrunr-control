package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.*;

/**
 * Represents a job execution with resolved parameters for display purposes.
 * <p>
 * This is a view model that wraps JobExecutionInfo by resolving external
 * parameter set references into their actual parameter values.
 * Used in UI controllers to display execution history with expanded parameters.
 */
public class JobExecutionInfoView {

    private final UUID jobId;
    private final String jobName;
    private final String jobType;
    private final JobStatus status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final BatchProgress batchProgress;
    private final Map<String, Object> displayParameters; // Resolved parameters for display
    private final Map<String, Object> metadata;
    private final boolean usesExternalParameters;

    private JobExecutionInfoView(UUID jobId, String jobName, String jobType, JobStatus status,
                                 Instant startedAt, Instant finishedAt, BatchProgress batchProgress,
                                 Map<String, Object> displayParameters, Map<String, Object> metadata,
                                 boolean usesExternalParameters) {
        this.jobId = Objects.requireNonNull(jobId, "Job ID must not be null");
        this.jobName = Objects.requireNonNull(jobName, "Job Name must not be null");
        this.jobType = Objects.requireNonNull(jobType, "Job Type must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.batchProgress = batchProgress;
        this.displayParameters = displayParameters != null ? new HashMap<>(displayParameters) : new HashMap<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.usesExternalParameters = usesExternalParameters;
    }

    /**
     * Creates a view from a JobExecutionInfo with resolved parameters.
     *
     * @param executionInfo          the source execution info
     * @param resolvedParameters     the resolved parameters (external parameters expanded)
     * @param usesExternalParameters whether this job uses external parameter storage
     * @return a new JobExecutionInfoView
     */
    public static JobExecutionInfoView from(JobExecutionInfo executionInfo, Map<String, Object> resolvedParameters,
                                            boolean usesExternalParameters) {
        return new JobExecutionInfoView(
                executionInfo.getJobId(),
                executionInfo.getJobName(),
                executionInfo.getJobType(),
                executionInfo.getStatus(),
                executionInfo.getStartedAt(),
                executionInfo.getFinishedAt().orElse(null),
                executionInfo.getBatchProgress().orElse(null),
                resolvedParameters,
                executionInfo.getMetadata(),
                usesExternalParameters
        );
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

    /**
     * Returns the resolved parameters for display.
     * If the job uses external parameters, these are the actual parameter values
     * loaded from the parameter set, not the parameterSetId.
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(displayParameters);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public boolean usesExternalParameters() {
        return usesExternalParameters;
    }

    @SuppressWarnings("unused") // Used in templates
    public boolean isBatchJob() {
        return batchProgress != null;
    }
}
