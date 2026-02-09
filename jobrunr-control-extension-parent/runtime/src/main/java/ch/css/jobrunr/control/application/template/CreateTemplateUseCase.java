package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Creates a new template job.
     *
     * @param jobType    Name of the job definition (e.g., fully qualified class name)
     * @param jobName    User-defined name for this template
     * @param parameters Parameter map for job execution
     * @return UUID of the created template job
     */
    public UUID execute(String jobType, String jobName, Map<String, String> parameters) {
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

        // Template jobs are always external trigger with no scheduled time and have the "template" label
        UUID templateId = jobSchedulerPort.scheduleJob(
                jobDefinition,
                jobName,
                jobParameters,
                true,                    // isExternalTrigger
                null,                    // scheduledAt - templates have no schedule
                List.of("template")      // labels
        );

        // Audit log
        auditLogger.logTemplateCreated(jobName, templateId, jobParameters);

        return templateId;
    }
}
