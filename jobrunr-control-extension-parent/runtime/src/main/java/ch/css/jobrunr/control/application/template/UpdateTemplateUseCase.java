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
 * Use Case: Updates an existing template job.
 * Template jobs are always updated with external trigger and the "template" label.
 */
@ApplicationScoped
public class UpdateTemplateUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public UpdateTemplateUseCase(
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
     * Updates an existing template job.
     *
     * @param templateId Template job ID
     * @param jobType    Name of the job definition (e.g., fully qualified class name)
     * @param jobName    User-defined name for this template
     * @param parameters Parameter map for job execution
     */
    public void execute(UUID templateId, String jobType, String jobName, Map<String, String> parameters) {
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);

        jobSchedulerPort.assertTemplateNameUnique(jobName, templateId);

        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        parameterStorageHelper.updateJobWithParameters(
                templateId, jobDefinition, jobType, jobName, convertedParameters,
                true,           // isExternalTrigger
                null,           // scheduledAt — helper resolves EXTERNAL_TRIGGER_DATE
                List.of("template")
        );

        auditLogger.logTemplateUpdated(jobName, templateId, convertedParameters);
    }
}
