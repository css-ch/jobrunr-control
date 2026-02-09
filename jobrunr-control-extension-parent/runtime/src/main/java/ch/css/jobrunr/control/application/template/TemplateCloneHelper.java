package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
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

    private static final Logger LOG = Logger.getLogger(TemplateCloneHelper.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final ParameterStorageService parameterStorageService;
    private final ParameterStorageHelper parameterStorageHelper;

    @Inject
    public TemplateCloneHelper(
            JobSchedulerPort jobSchedulerPort,
            ParameterStorageService parameterStorageService,
            ParameterStorageHelper parameterStorageHelper) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStorageService = parameterStorageService;
        this.parameterStorageHelper = parameterStorageHelper;
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

        LOG.infof("Cloning template job %s (%s)", templateId, sourceJob.jobName());

        // Create a new name for the clone
        String newJobName = generateJobName(sourceJob.jobName(), postfix);

        UUID newJobId;

        if (sourceJob.jobDefinition().usesExternalParameters()) {
            // TWO-PHASE APPROACH for external parameters
            LOG.debugf("Using two-phase approach for job with external parameters");

            // Phase 1: Create job with empty/reference parameters
            Map<String, Object> emptyParams = Map.of();
            if (additionalLabels != null && !additionalLabels.isEmpty()) {
                newJobId = jobSchedulerPort.scheduleJob(
                        sourceJob.jobDefinition(),
                        newJobName,
                        emptyParams,
                        true,              // isExternalTrigger
                        null,              // scheduledAt
                        additionalLabels
                );
            } else {
                newJobId = jobSchedulerPort.scheduleJob(
                        sourceJob.jobDefinition(),
                        newJobName,
                        emptyParams,
                        true,              // isExternalTrigger
                        null               // scheduledAt
                );
            }

            // Phase 2: Load template parameters, clone them, and store with new job ID
            UUID templateParameterSetId = templateId; // Template's parameter set ID = template job ID
            var templateParamSet = parameterStorageService.findById(templateParameterSetId);

            if (templateParamSet.isPresent()) {
                Map<String, Object> clonedParameters = new HashMap<>(templateParamSet.get().parameters());

                // Apply overrides if provided
                if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
                    clonedParameters.putAll(parameterOverrides);
                    LOG.infof("Applied %s parameter override(s)", parameterOverrides.size());
                }

                // Store parameters with new job ID
                ParameterSet newParamSet = ParameterSet.create(
                        newJobId,
                        sourceJob.jobDefinition().jobType(),
                        clonedParameters
                );
                parameterStorageService.store(newParamSet);
                LOG.infof("Stored cloned parameter set with ID: %s", newJobId);

                // Update job to reference the parameter set
                Map<String, Object> paramReference = parameterStorageHelper.createParameterReference(
                        newJobId,
                        sourceJob.jobDefinition()
                );
                jobSchedulerPort.updateJobParameters(newJobId, paramReference);
                LOG.infof("Updated job %s with parameter reference", newJobId);
            } else {
                LOG.warnf("Template parameter set not found for template: %s", templateId);
            }

        } else {
            // INLINE PARAMETERS: Use existing single-phase approach
            LOG.debugf("Using single-phase approach for job with inline parameters");

            // Merge parameters: start with template parameters, then apply overrides
            Map<String, Object> mergedParameters = new HashMap<>(sourceJob.parameters());
            if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
                mergedParameters.putAll(parameterOverrides);
                LOG.infof("Applied %s parameter override(s)", parameterOverrides.size());
            }

            // Schedule the new job
            if (additionalLabels != null && !additionalLabels.isEmpty()) {
                newJobId = jobSchedulerPort.scheduleJob(
                        sourceJob.jobDefinition(),
                        newJobName,
                        mergedParameters,
                        true,              // isExternalTrigger
                        null,              // scheduledAt
                        additionalLabels
                );
            } else {
                newJobId = jobSchedulerPort.scheduleJob(
                        sourceJob.jobDefinition(),
                        newJobName,
                        mergedParameters,
                        true,              // isExternalTrigger
                        null               // scheduledAt
                );
            }
        }

        LOG.infof("Created cloned job with ID: %s and name: %s", newJobId, newJobName);

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
        String effectivePostfix = postfix;
        if (effectivePostfix == null || effectivePostfix.isBlank()) {
            effectivePostfix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        return baseJobName + "-" + effectivePostfix;
    }
}
