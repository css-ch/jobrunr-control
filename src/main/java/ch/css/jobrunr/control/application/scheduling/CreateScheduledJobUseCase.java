package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Creates a scheduled job.
 * Validates parameter types and handles "External Trigger" logic.
 */
@ApplicationScoped
public class CreateScheduledJobUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;

    @Inject
    public CreateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobSchedulerPort jobSchedulerPort,
            JobParameterValidator validator) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobSchedulerPort = jobSchedulerPort;
        this.validator = validator;
    }

    /**
     * Creates a new scheduled job.
     *
     * @param jobType           Name of the job definition (e.g., fully qualified class name)
     * @param jobName           User-defined name for this job instance
     * @param parameters        Parameter map for job execution
     * @param scheduledAt       Scheduled execution time (null for external trigger)
     * @param isExternalTrigger Whether the job should be externally triggered
     * @return UUID of the scheduled job
     * @throws JobNotFoundException when the job is not found
     */
    public UUID execute(String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger) {

        // Load job definition
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(jobType);
        if (jobDefOpt.isEmpty()) {
            throw new JobNotFoundException("Job type '" + jobType + "' not found");
        }

        JobDefinition jobDefinition = jobDefOpt.get();

        // Validate parameters
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        // Schedule job
        return jobSchedulerPort.scheduleJob(jobDefinition, jobName, convertedParameters, isExternalTrigger, scheduledAt);
    }

    /**
     * Exception for jobs not found.
     */
    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(String message) {
            super(message);
        }
    }
}

