package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Updates an existing template job.
 * Template jobs are always updated with external trigger and the "template" label.
 */
@ApplicationScoped
public class UpdateTemplateUseCase {

    // Date for externally triggerable jobs (31.12.2999)
    private static final Instant EXTERNAL_TRIGGER_DATE =
            LocalDate.of(2999, 12, 31)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobSchedulerPort jobSchedulerPort;
    private final JobParameterValidator validator;
    private final ParameterStorageHelper parameterStorageHelper;

    @Inject
    public UpdateTemplateUseCase(
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

        // Validate parameters
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

        // Prepare job parameters (handles inline vs external storage)
        Map<String, Object> jobParameters = parameterStorageHelper.prepareJobParameters(
                jobDefinition, jobType, jobName, convertedParameters);

        // Template jobs are always external trigger with no scheduled time and maintain the "template" label
        jobSchedulerPort.updateJob(
                templateId,
                jobDefinition,
                jobName,
                jobParameters,
                true,                    // isExternalTrigger
                EXTERNAL_TRIGGER_DATE,   // External trigger uses special date
                List.of("template")      // labels
        );
    }
}
