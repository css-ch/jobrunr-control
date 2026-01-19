package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Updates a scheduled job.
 * Updates the job directly without deleting and recreating.
 */
@ApplicationScoped
public class UpdateScheduledJobUseCase {

    // Date for externally triggerable jobs (31.12.2999)
    private static final Instant EXTERNAL_TRIGGER_DATE =
            LocalDate.of(2999, 12, 31)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;

    @Inject
    public UpdateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobSchedulerPort jobSchedulerPort,
            JobParameterValidator validator) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobSchedulerPort = jobSchedulerPort;
        this.validator = validator;
    }

    /**
     * Updates a scheduled job directly (without deleting and recreating it).
     *
     * @param jobId             ID of the job to update
     * @param jobType           Type of the job (full class name)
     * @param jobName           Name of the job
     * @param parameters        New parameter map
     * @param scheduledAt       New time of the scheduled execution
     * @param isExternalTrigger Whether the job should be triggered externally
     * @return UUID of the updated job (same ID as input)
     * @throws JobNotFoundException if the job is not found
     */
    public UUID execute(UUID jobId, String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger) {

        // Load job definition
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(jobType);
        if (jobDefOpt.isEmpty()) {
            throw new JobNotFoundException("JobDefinition for type '" + jobType + "' not found");
        }
        JobDefinition jobDefinition = jobDefOpt.get();

        // Validate parameters
        Map<String, Object> convertedParameter = validator.convertAndValidate(jobDefinition, parameters);

        // Determine time
        Instant effectiveScheduledAt = isExternalTrigger
                ? EXTERNAL_TRIGGER_DATE
                : scheduledAt;

        if (effectiveScheduledAt == null) {
            throw new IllegalArgumentException(
                    "scheduledAt must not be null if isExternalTrigger is false"
            );
        }

        // Update job directly (more efficient method)
        jobSchedulerPort.updateJob(jobId, jobDefinition, jobName, convertedParameter, isExternalTrigger, effectiveScheduledAt);

        // Return the original job ID
        return jobId;
    }

    /**
     * Exception for not found jobs.
     */
    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(String message) {
            super(message);
        }
    }
}
