package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
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

    private static final Logger LOG = Logger.getLogger(CreateScheduledJobUseCase.class);

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public CreateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobSchedulerPort jobSchedulerPort,
            JobParameterValidator validator,
            ParameterStorageHelper parameterStorageHelper,
            AuditLoggerHelper auditLogger) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobSchedulerPort = jobSchedulerPort;
        this.validator = validator;
        this.parameterStorageHelper = parameterStorageHelper;
        this.auditLogger = auditLogger;
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

        UUID jobId;

        if (jobDefinition.usesExternalParameters()) {
            // TWO-PHASE APPROACH: Create job first, then store parameters with job UUID
            LOG.debugf("Using two-phase approach for job with external parameters: %s", jobType);

            // Phase 1: Create job with empty parameters
            Map<String, Object> emptyParams = Map.of();
            jobId = jobSchedulerPort.scheduleJob(jobDefinition, jobName, emptyParams, isExternalTrigger, scheduledAt, additionalLabels);

            // Phase 2: Store parameters using job UUID as parameter set ID
            parameterStorageHelper.storeParametersForJob(jobId, jobDefinition, jobType, convertedParameters);

            // Phase 3: Update job with parameter reference
            Map<String, Object> paramReference = parameterStorageHelper.createParameterReference(jobId, jobDefinition);
            jobSchedulerPort.updateJobParameters(jobId, paramReference);

            LOG.infof("Created job with external parameters: %s (ID: %s)", jobType, jobId);
        } else {
            // SINGLE-PHASE APPROACH: Inline parameters
            LOG.debugf("Using single-phase approach for job with inline parameters: %s", jobType);
            jobId = jobSchedulerPort.scheduleJob(jobDefinition, jobName, convertedParameters, isExternalTrigger, scheduledAt, additionalLabels);
        }

        // Audit log
        auditLogger.logJobCreated(jobName, jobId, convertedParameters);

        return jobId;
    }
}

