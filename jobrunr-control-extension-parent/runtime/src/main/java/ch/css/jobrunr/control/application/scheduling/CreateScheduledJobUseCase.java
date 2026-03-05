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
 * Use Case: Creates a scheduled job.
 * Validates parameter types and handles "External Trigger" logic.
 * Automatically stores parameters externally if job uses @JobParameterSet annotation.
 */
@ApplicationScoped
public class CreateScheduledJobUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public CreateScheduledJobUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobParameterValidator validator,
            ParameterStorageHelper parameterStorageHelper,
            AuditLoggerHelper auditLogger) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.validator = validator;
        this.parameterStorageHelper = parameterStorageHelper;
        this.auditLogger = auditLogger;
    }

    public UUID execute(String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger) {
        return execute(jobType, jobName, parameters, scheduledAt, isExternalTrigger, null);
    }

    public UUID execute(String jobType, String jobName, Map<String, String> parameters,
                        Instant scheduledAt, boolean isExternalTrigger, List<String> additionalLabels) {

        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        UUID jobId = parameterStorageHelper.scheduleJobWithParameters(
                jobDefinition, jobType, jobName, convertedParameters, isExternalTrigger, scheduledAt, additionalLabels);

        auditLogger.logJobCreated(jobName, jobId, convertedParameters);
        return jobId;
    }
}
