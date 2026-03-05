package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class to prepare job parameters for scheduling.
 * Handles the distinction between inline and external parameter storage,
 * including the two-phase creation protocol for external parameters.
 */
@ApplicationScoped
public class ParameterStorageHelper {

    private static final Logger LOG = Logger.getLogger(ParameterStorageHelper.class);

    /** Placeholder date used for externally triggerable jobs (31.12.2999). */
    static final Instant EXTERNAL_TRIGGER_DATE =
            LocalDate.of(2999, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant();

    private final JobSchedulerPort jobSchedulerPort;
    private final ParameterStorageService parameterStorageService;

    @Inject
    public ParameterStorageHelper(JobSchedulerPort jobSchedulerPort, ParameterStorageService parameterStorageService) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStorageService = parameterStorageService;
    }

    /**
     * Schedules a job and stores its parameters, handling external vs inline storage automatically.
     * For jobs with external parameters, uses a two-phase approach:
     * Phase 1 — create job with empty params to obtain the UUID,
     * Phase 2 — store parameters using that UUID,
     * Phase 3 — update job with empty param map (parameters accessed via job UUID at runtime).
     *
     * @param jobDefinition       The job definition
     * @param jobType             The job type (for storage identification and error messaging)
     * @param jobName             User-defined name for this job instance
     * @param convertedParameters Validated and converted parameters
     * @param isExternalTrigger   Whether the job should be externally triggered
     * @param scheduledAt         Scheduled execution time (null for external trigger)
     * @param labels              Additional labels to apply to the job
     * @return UUID of the scheduled job
     * @throws IllegalStateException if external storage is required but not configured
     */
    public UUID scheduleJobWithParameters(
            JobDefinition jobDefinition,
            String jobType,
            String jobName,
            Map<String, Object> convertedParameters,
            boolean isExternalTrigger,
            Instant scheduledAt,
            List<String> labels) {

        validateExternalStorage(jobDefinition, jobType);

        if (jobDefinition.usesExternalParameters()) {
            LOG.debugf("Using two-phase approach for job with external parameters: %s", jobType);
            UUID jobId = jobSchedulerPort.scheduleJob(jobDefinition, jobName, Map.of(), isExternalTrigger, scheduledAt, labels);
            storeParametersForJob(jobId, jobType, convertedParameters);
            jobSchedulerPort.updateJobParameters(jobId, Map.of());
            LOG.infof("Created job with external parameters: %s (ID: %s)", jobType, jobId);
            return jobId;
        } else {
            LOG.debugf("Using single-phase approach for job with inline parameters: %s", jobType);
            return jobSchedulerPort.scheduleJob(jobDefinition, jobName, convertedParameters, isExternalTrigger, scheduledAt, labels);
        }
    }

    /**
     * Updates a job and its parameters, handling external vs inline storage automatically.
     * For jobs with external parameters, updates the parameter set in-place and then
     * updates the job metadata with an empty parameter map.
     *
     * @param jobId               The UUID of the job to update
     * @param jobDefinition       The job definition
     * @param jobType             The job type (for storage identification and error messaging)
     * @param jobName             New job name
     * @param convertedParameters Validated and converted parameters
     * @param isExternalTrigger   Whether the job should be externally triggered
     * @param scheduledAt         New scheduled execution time
     * @param labels              Additional labels to apply to the job
     * @throws IllegalStateException if external storage is required but not configured
     */
    public void updateJobWithParameters(
            UUID jobId,
            JobDefinition jobDefinition,
            String jobType,
            String jobName,
            Map<String, Object> convertedParameters,
            boolean isExternalTrigger,
            Instant scheduledAt,
            List<String> labels) {

        if (!isExternalTrigger && scheduledAt == null) {
            throw new IllegalArgumentException("scheduledAt must not be null if isExternalTrigger is false");
        }
        Instant effectiveScheduledAt = isExternalTrigger ? EXTERNAL_TRIGGER_DATE : scheduledAt;

        validateExternalStorage(jobDefinition, jobType);

        if (jobDefinition.usesExternalParameters()) {
            LOG.debugf("Updating job with external parameters: %s (ID: %s)", jobType, jobId);
            updateParametersForJob(jobId, jobType, convertedParameters);
            jobSchedulerPort.updateJob(jobId, jobDefinition, jobName, Map.of(), isExternalTrigger, effectiveScheduledAt, labels);
            LOG.infof("Updated job with external parameters: %s (ID: %s)", jobType, jobId);
        } else {
            LOG.debugf("Using single-phase update for job with inline parameters: %s (ID: %s)", jobType, jobId);
            jobSchedulerPort.updateJob(jobId, jobDefinition, jobName, convertedParameters, isExternalTrigger, effectiveScheduledAt, labels);
        }
    }

    private void validateExternalStorage(JobDefinition jobDefinition, String jobType) {
        if (!jobDefinition.usesExternalParameters()) {
            return;
        }
        if (!parameterStorageService.isExternalStorageAvailable()) {
            throw new IllegalStateException(
                    "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                            "but external storage is not configured. " +
                            "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
        }
    }

    private void storeParametersForJob(UUID jobId, String jobType, Map<String, Object> convertedParameters) {
        ParameterSet parameterSet = ParameterSet.create(jobId, jobType, convertedParameters);
        parameterStorageService.store(parameterSet);
        LOG.infof("Stored parameters externally with ID: %s (job UUID) for job type: %s", jobId, jobType);
    }

    private void updateParametersForJob(UUID jobId, String jobType, Map<String, Object> convertedParameters) {
        ParameterSet parameterSet = ParameterSet.create(jobId, jobType, convertedParameters);
        parameterStorageService.update(parameterSet);
        LOG.infof("Updated parameters externally with ID: %s (job UUID) for job type: %s", jobId, jobType);
    }
}
