package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Creates a scheduled job.
 * Validates parameter types and handles "External Trigger" logic.
 * Automatically stores parameters externally if job uses @JobParameterSet annotation.
 */
@ApplicationScoped
public class CreateScheduledJobUseCase {

    private static final Logger log = Logger.getLogger(CreateScheduledJobUseCase.class);

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;

    @Inject
    public CreateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobSchedulerPort jobSchedulerPort,
            JobParameterValidator validator,
            ParameterStorageHelper parameterStorageHelper) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobSchedulerPort = jobSchedulerPort;
        this.validator = validator;
        this.parameterStorageHelper = parameterStorageHelper;
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
        return execute(jobType, jobName, parameters, scheduledAt, isExternalTrigger, null);
    }

    /**
     * Creates a new scheduled job with additional labels.
     *
     * @param jobType           Name of the job definition (e.g., fully qualified class name)
     * @param jobName           User-defined name for this job instance
     * @param parameters        Parameter map for job execution
     * @param scheduledAt       Scheduled execution time (null for external trigger)
     * @param isExternalTrigger Whether the job should be externally triggered
     * @param additionalLabels  Additional labels to add to the job
     * @return UUID of the scheduled job
     * @throws JobNotFoundException  when the job is not found
     * @throws IllegalStateException when job requires external storage but it's not configured
     */
    public UUID execute(String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger, java.util.List<String> additionalLabels) {

        // Load job definition
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(jobType);
        if (jobDefOpt.isEmpty()) {
            throw new JobNotFoundException("Job type '" + jobType + "' not found");
        }

        JobDefinition jobDefinition = jobDefOpt.get();

        // Validate and convert parameters
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        // Prepare job parameters (handles inline vs external storage)
        Map<String, Object> jobParameters = parameterStorageHelper.prepareJobParameters(
                jobDefinition, jobType, jobName, convertedParameters);

        // Schedule job
        return jobSchedulerPort.scheduleJob(jobDefinition, jobName, jobParameters, isExternalTrigger, scheduledAt, additionalLabels);
    }
}

