package ch.css.jobrunr.control.application.template;

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

    @Inject
    public TemplateCloneHelper(
            JobSchedulerPort jobSchedulerPort,
            ParameterStorageService parameterStorageService) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStorageService = parameterStorageService;
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
        validateTemplateId(templateId);

        ScheduledJobInfo sourceJob = loadTemplateJob(templateId);
        String newJobName = generateJobName(sourceJob.jobName(), postfix);

        LOG.infof("Cloning template job %s (%s)", templateId, sourceJob.jobName());

        UUID newJobId = sourceJob.jobDefinition().usesExternalParameters()
                ? cloneTemplateWithExternalParameters(templateId, sourceJob, newJobName, parameterOverrides, additionalLabels)
                : cloneTemplateWithInlineParameters(sourceJob, newJobName, parameterOverrides, additionalLabels);

        LOG.infof("Created cloned job with ID: %s and name: %s", newJobId, newJobName);

        return newJobId;
    }

    private void validateTemplateId(UUID templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("templateId must not be null");
        }
    }

    private ScheduledJobInfo loadTemplateJob(UUID templateId) {
        ScheduledJobInfo sourceJob = jobSchedulerPort.getScheduledJobById(templateId);
        if (sourceJob == null) {
            throw new IllegalArgumentException("Template job not found: " + templateId);
        }
        return sourceJob;
    }

    private UUID cloneTemplateWithExternalParameters(UUID templateId, ScheduledJobInfo sourceJob, String newJobName,
                                                     Map<String, Object> parameterOverrides, List<String> additionalLabels) {
        LOG.debugf("Using two-phase approach for job with external parameters");

        // Phase 1: Create job with empty/reference parameters
        UUID newJobId = scheduleJobWithLabels(sourceJob.jobDefinition(), newJobName, Map.of(), additionalLabels);

        // Phase 2: Load template parameters, clone them, and store with new job ID
        var templateParamSet = parameterStorageService.findById(templateId);

        if (templateParamSet.isPresent()) {
            Map<String, Object> clonedParameters = new HashMap<>(templateParamSet.get().parameters());
            applyParameterOverrides(clonedParameters, parameterOverrides);

            // Store parameters with new job ID
            ParameterSet newParamSet = ParameterSet.create(
                    newJobId,
                    sourceJob.jobDefinition().jobType(),
                    clonedParameters
            );
            parameterStorageService.store(newParamSet);
            LOG.infof("Stored cloned parameter set with ID: %s", newJobId);

            // Update job with empty parameter map (parameters are accessed via job UUID)
            jobSchedulerPort.updateJobParameters(newJobId, Map.of());
            LOG.infof("Updated job %s with parameter reference", newJobId);
        } else {
            LOG.warnf("Template parameter set not found for template: %s", templateId);
        }

        return newJobId;
    }

    private UUID cloneTemplateWithInlineParameters(ScheduledJobInfo sourceJob, String newJobName,
                                                   Map<String, Object> parameterOverrides, List<String> additionalLabels) {
        LOG.debugf("Using single-phase approach for job with inline parameters");

        // Merge parameters: start with template parameters, then apply overrides
        Map<String, Object> mergedParameters = new HashMap<>(sourceJob.parameters());
        applyParameterOverrides(mergedParameters, parameterOverrides);

        // Schedule the new job
        return scheduleJobWithLabels(sourceJob.jobDefinition(), newJobName, mergedParameters, additionalLabels);
    }

    private void applyParameterOverrides(Map<String, Object> parameters, Map<String, Object> overrides) {
        if (overrides != null && !overrides.isEmpty()) {
            parameters.putAll(overrides);
            LOG.infof("Applied %s parameter override(s)", overrides.size());
        }
    }

    private UUID scheduleJobWithLabels(ch.css.jobrunr.control.domain.JobDefinition jobDefinition, String jobName,
                                       Map<String, Object> parameters, List<String> additionalLabels) {
        if (additionalLabels != null && !additionalLabels.isEmpty()) {
            return jobSchedulerPort.scheduleJob(
                    jobDefinition,
                    jobName,
                    parameters,
                    true,              // isExternalTrigger
                    null,              // scheduledAt
                    additionalLabels
            );
        } else {
            return jobSchedulerPort.scheduleJob(
                    jobDefinition,
                    jobName,
                    parameters,
                    true,              // isExternalTrigger
                    null               // scheduledAt
            );
        }
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
