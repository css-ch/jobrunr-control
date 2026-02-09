package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Updates an existing template job.
 * Template jobs are always updated with external trigger and the "template" label.
 */
@ApplicationScoped
public class UpdateTemplateUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateTemplateUseCase.class);

    // Date for externally triggerable jobs (31.12.2999)
    private static final Instant EXTERNAL_TRIGGER_DATE =
            LocalDate.of(2999, 12, 31)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;
    private final ParameterStorageService parameterStorageService;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public UpdateTemplateUseCase(
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobSchedulerPort jobSchedulerPort,
            JobParameterValidator validator,
            ParameterStorageHelper parameterStorageHelper,
            ParameterStorageService parameterStorageService,
            AuditLoggerHelper auditLogger) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobSchedulerPort = jobSchedulerPort;
        this.validator = validator;
        this.parameterStorageHelper = parameterStorageHelper;
        this.parameterStorageService = parameterStorageService;
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
        // Load job definition
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(jobType);
        if (jobDefOpt.isEmpty()) {
            throw new JobNotFoundException("JobDefinition for type '" + jobType + "' not found");
        }

        JobDefinition jobDefinition = jobDefOpt.get();

        // Validate and convert parameters
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        if (jobDefinition.usesExternalParameters()) {
            // THREE-PHASE UPDATE: Delete old params, update job, store new params
            LOG.debugf("Using three-phase update for template with external parameters: %s (ID: %s)", jobType, templateId);

            // Phase 1: Delete old parameter set (if exists)
            if (parameterStorageService.isExternalStorageAvailable()) {
                parameterStorageService.deleteById(templateId);
                LOG.debugf("Deleted old parameter set for template: %s", templateId);
            }

            // Phase 2: Update template with empty parameters
            Map<String, Object> emptyParams = Map.of();
            jobSchedulerPort.updateJob(
                    templateId,
                    jobDefinition,
                    jobName,
                    emptyParams,
                    true,                              // isExternalTrigger
                    EXTERNAL_TRIGGER_DATE,             // External trigger uses special date
                    java.util.List.of("template")      // Always add "template" label
            );

            // Phase 3: Store new parameters with same template UUID
            parameterStorageHelper.storeParametersForJob(templateId, jobDefinition, jobType, convertedParameters);

            // Phase 4: Update template with parameter reference
            Map<String, Object> paramReference = parameterStorageHelper.createParameterReference(templateId, jobDefinition);
            jobSchedulerPort.updateJobParameters(templateId, paramReference);

            LOG.infof("Updated template with external parameters: %s (ID: %s)", jobType, templateId);
        } else {
            // SINGLE-PHASE UPDATE: Inline parameters
            LOG.debugf("Using single-phase update for template with inline parameters: %s (ID: %s)", jobType, templateId);
            jobSchedulerPort.updateJob(
                    templateId,
                    jobDefinition,
                    jobName,
                    convertedParameters,
                    true,                              // isExternalTrigger
                    EXTERNAL_TRIGGER_DATE,             // External trigger uses special date
                    java.util.List.of("template")      // Always add "template" label
            );
        }

        // Audit log
        auditLogger.logTemplateUpdated(jobName, templateId, convertedParameters);
    }
}
