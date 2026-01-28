package ch.css.jobrunr.control.application.template;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for cloning template jobs.
 * Provides common functionality for creating new jobs based on template configurations.
 */
@ApplicationScoped
public class TemplateCloneHelper {

    private static final Logger log = Logger.getLogger(TemplateCloneHelper.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public TemplateCloneHelper(
            JobSchedulerPort jobSchedulerPort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    /**
     * Clones a template job with optional parameter overrides and labels.
     *
     * @param templateId         ID of the template job to clone
     * @param postfix            Optional postfix for the cloned job name (defaults to current date in yyyyMMdd format)
     * @param parameterOverrides Optional parameters to override in the cloned job (can be null)
     * @param additionalLabels   Additional labels to apply to the cloned job (can be null)
     * @return UUID of the newly created job
     * @throws IllegalArgumentException if the template job is not found
     */
    public UUID cloneTemplate(UUID templateId, String postfix, Map<String, Object> parameterOverrides, List<String> additionalLabels) {
        if (templateId == null) {
            throw new IllegalArgumentException("templateId must not be null");
        }

        // Get the template job
        ScheduledJobInfo sourceJob = jobSchedulerPort.getScheduledJobById(templateId);
        if (sourceJob == null) {
            throw new IllegalArgumentException("Template job not found: " + templateId);
        }

        log.infof("Cloning template job %s (%s)", templateId, sourceJob.getJobName());

        // Get the job definition for the template job's type
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.findJobByType(sourceJob.getJobType())
                .orElseThrow(() -> new IllegalArgumentException("Job definition not found for type: " + sourceJob.getJobType()));

        // Merge parameters: start with template parameters, then apply overrides
        Map<String, Object> mergedParameters = new HashMap<>(sourceJob.getParameters());
        if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
            mergedParameters.putAll(parameterOverrides);
            log.infof("Applied %s parameter override(s)", parameterOverrides.size());
        }

        // Create a new name for the clone
        String newJobName = generateJobName(sourceJob.getJobName(), postfix);

        // Schedule the new job
        UUID newJobId;
        if (additionalLabels != null && !additionalLabels.isEmpty()) {
            newJobId = jobSchedulerPort.scheduleJob(
                    jobDefinition,
                    newJobName,
                    mergedParameters,
                    true,              // isExternalTrigger
                    null,              // scheduledAt
                    additionalLabels
            );
        } else {
            newJobId = jobSchedulerPort.scheduleJob(
                    jobDefinition,
                    newJobName,
                    mergedParameters,
                    true,              // isExternalTrigger
                    null               // scheduledAt
            );
        }

        log.infof("Created cloned job with ID: %s and name: %s", newJobId, newJobName);

        return newJobId;
    }

    /**
     * Generates a new job name by appending a postfix.
     *
     * @param baseJobName Base job name
     * @param postfix     Optional postfix (defaults to current date in yyyyMMdd format if null or blank)
     * @return Generated job name
     */
    private String generateJobName(String baseJobName, String postfix) {
        if (postfix == null || postfix.isBlank()) {
            postfix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        return baseJobName + "-" + postfix;
    }
}
