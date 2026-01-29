package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.*;

/**
 * Represents a scheduled job with resolved parameters for display purposes.
 * <p>
 * This is a view model that extends ScheduledJobInfo by resolving external
 * parameter set references into their actual parameter values.
 * Used in UI controllers to display jobs with expanded parameters.
 */
public class ScheduledJobInfoView {

    private final UUID jobId;
    private final String jobName;
    private final String jobType;
    private final Instant scheduledAt;
    private final Map<String, Object> displayParameters; // Resolved parameters for display
    private final boolean isExternallyTriggerable;
    private final List<String> labels;
    private final boolean usesExternalParameters;

    private ScheduledJobInfoView(UUID jobId, String jobName, String jobType, Instant scheduledAt,
                                 Map<String, Object> displayParameters, boolean isExternallyTriggerable,
                                 List<String> labels, boolean usesExternalParameters) {
        this.jobId = Objects.requireNonNull(jobId, "Job ID must not be null");
        this.jobName = Objects.requireNonNull(jobName, "Job Name must not be null");
        this.jobType = Objects.requireNonNull(jobType, "Job Type must not be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "Scheduled at must not be null");
        this.displayParameters = displayParameters != null ? new HashMap<>(displayParameters) : new HashMap<>();
        this.isExternallyTriggerable = isExternallyTriggerable;
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        this.usesExternalParameters = usesExternalParameters;
    }

    /**
     * Creates a view from a ScheduledJobInfo with resolved parameters.
     *
     * @param jobInfo                the source job info
     * @param resolvedParameters     the resolved parameters (external parameters expanded)
     * @param usesExternalParameters whether this job uses external parameter storage
     * @return a new ScheduledJobInfoView
     */
    public static ScheduledJobInfoView from(ScheduledJobInfo jobInfo, Map<String, Object> resolvedParameters,
                                            boolean usesExternalParameters) {
        return new ScheduledJobInfoView(
                jobInfo.getJobId(),
                jobInfo.getJobName(),
                jobInfo.getJobType(),
                jobInfo.getScheduledAt(),
                resolvedParameters,
                jobInfo.isExternallyTriggerable(),
                jobInfo.getLabels(),
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

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    /**
     * Returns the resolved parameters for display.
     * If the job uses external parameters, these are the actual parameter values
     * loaded from the parameter set, not the parameterSetId.
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(displayParameters);
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

    public boolean usesExternalParameters() {
        return usesExternalParameters;
    }

    public int getParameterCount() {
        return displayParameters.size();
    }
}
