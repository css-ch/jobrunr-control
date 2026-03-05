package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Updates a scheduled job.
 * Updates the job directly without deleting and recreating.
 * Handles external parameter storage if job uses @JobParameterSet annotation.
 */
@ApplicationScoped
public class UpdateScheduledJobUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public UpdateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobParameterValidator validator,
            ParameterStorageHelper parameterStorageHelper,
            AuditLoggerHelper auditLogger) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.validator = validator;
        this.parameterStorageHelper = parameterStorageHelper;
        this.auditLogger = auditLogger;
    }

    public UUID execute(UUID jobId, String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger) {
        return execute(jobId, jobType, jobName, parameters, scheduledAt, isExternalTrigger, null);
    }

    public UUID execute(UUID jobId, String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger, List<String> additionalLabels) {

        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        parameterStorageHelper.updateJobWithParameters(
                jobId, jobDefinition, jobType, jobName, convertedParameters, isExternalTrigger, scheduledAt, additionalLabels);

        auditLogger.logJobUpdated(jobName, jobId, convertedParameters);
        return jobId;
    }
}
