package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Creates a new template job.
 * Template jobs are always created with external trigger and the "template" label.
 */
@ApplicationScoped
public class CreateTemplateUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public CreateTemplateUseCase(
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

    public UUID execute(String jobType, String jobName, Map<String, String> parameters) {
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);

        jobSchedulerPort.assertTemplateNameUnique(jobName, null);

        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        UUID templateId = parameterStorageHelper.scheduleJobWithParameters(
                jobDefinition, jobType, jobName, convertedParameters,
                true,               // isExternalTrigger
                null,               // scheduledAt — templates have no schedule
                List.of("template")
        );

        auditLogger.logTemplateCreated(jobName, templateId, convertedParameters);
        return templateId;
    }
}
