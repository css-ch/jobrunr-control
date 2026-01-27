package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Executes a template job by cloning it and starting the clone immediately.
 * Template jobs cannot be executed directly and must be cloned first.
 */
@ApplicationScoped
public class ExecuteTemplateUseCase {

    private static final Logger log = Logger.getLogger(ExecuteTemplateUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public ExecuteTemplateUseCase(
            JobSchedulerPort jobSchedulerPort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    /**
     * Executes a template job by cloning it and starting the clone immediately with optional parameter overrides.
     *
     * @param templateId         ID of the template job to execute
     * @param postfix            Optional postfix for the cloned job name (defaults to current date in yyyyMMdd format)
     * @param parameterOverrides Optional parameters to override in the cloned job
     * @return UUID of the newly created and started job
     * @throws IllegalArgumentException if the template job is not found
     */
    public UUID execute(UUID templateId, String postfix, Map<String, Object> parameterOverrides) {
        if (templateId == null) {
            throw new IllegalArgumentException("templateId must not be null");
        }

        // Get the template job
        ScheduledJobInfo sourceJob = jobSchedulerPort.getScheduledJobById(templateId);
        if (sourceJob == null) {
            throw new IllegalArgumentException("Template job not found: " + templateId);
        }

        log.infof("Executing template job %s (%s) by cloning", templateId, sourceJob.getJobName());

        // Get the job definition for the template job's type
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(sourceJob.getJobType());
        if (jobDefOpt.isEmpty()) {
            throw new IllegalArgumentException("Job definition not found for type: " + sourceJob.getJobType());
        }

        JobDefinition jobDefinition = jobDefOpt.get();

        // Merge parameters: start with template parameters, then apply overrides
        Map<String, Object> mergedParameters = new HashMap<>(sourceJob.getParameters());
        if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
            mergedParameters.putAll(parameterOverrides);
            log.infof("Applied %s parameter override(s)", parameterOverrides.size());
        }

        // Create a new job name for the clone
        if (postfix == null || postfix.isBlank()) {
            postfix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        String newJobName = sourceJob.getJobName() + "-" + postfix;

        // Schedule the new job (as externally triggerable so we can start it immediately)
        UUID newJobId = jobSchedulerPort.scheduleJob(
                jobDefinition,
                newJobName,
                mergedParameters,
                true, // isExternalTrigger
                null  // scheduledAt
        );

        log.infof("Created cloned job with ID: %s", newJobId);

        // Start the job immediately
        jobSchedulerPort.executeJobNow(newJobId, parameterOverrides);

        log.infof("Started cloned job %s", newJobId);

        return newJobId;
    }
}
