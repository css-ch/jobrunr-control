package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.*;
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
    private final ParameterStorageService parameterStorageService;

    @Inject
    public CreateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobSchedulerPort jobSchedulerPort,
            JobParameterValidator validator,
            ParameterStorageService parameterStorageService) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobSchedulerPort = jobSchedulerPort;
        this.validator = validator;
        this.parameterStorageService = parameterStorageService;
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

        Map<String, Object> jobParameters;

        // NEW LOGIC: Check if job uses external parameters (@JobParameterSet annotation)
        if (jobDefinition.usesExternalParameters()) {
            // Validate external storage is available
            if (!parameterStorageService.isExternalStorageAvailable()) {
                throw new IllegalStateException(
                        "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                                "but external storage is not configured. " +
                                "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
            }

            // Store parameters externally
            UUID parameterSetId = UUID.randomUUID();
            ParameterSet parameterSet = ParameterSet.create(parameterSetId, jobType, convertedParameters);
            parameterStorageService.store(parameterSet);

            // Create job parameters with ONLY the parameter set ID in the annotated field
            jobParameters = Map.of(jobDefinition.parameterSetFieldName(), parameterSetId.toString());

            log.infof("Stored parameters externally with ID: %s for job: %s", parameterSetId, jobName);
        } else {
            // INLINE: Use converted parameters directly
            jobParameters = convertedParameters;
        }

        // Schedule job
        return jobSchedulerPort.scheduleJob(jobDefinition, jobName, jobParameters, isExternalTrigger, scheduledAt, additionalLabels);
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

