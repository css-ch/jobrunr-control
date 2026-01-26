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
 * Use Case: Clones a scheduled job and starts it immediately with optional parameter overrides.
 */
@ApplicationScoped
public class CloneAndStartJobUseCase {

    private static final Logger log = Logger.getLogger(CloneAndStartJobUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public CloneAndStartJobUseCase(
            JobSchedulerPort jobSchedulerPort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    /**
     * Clones an existing job and starts it immediately.
     *
     * @param cloneFromId        ID of the job to clone
     * @param parameterOverrides Optional parameters to override in the cloned job
     * @return UUID of the newly created and started job
     * @throws IllegalArgumentException if the source job is not found
     */
    public UUID execute(UUID cloneFromId, String suffix, Map<String, Object> parameterOverrides) {
        if (cloneFromId == null) {
            throw new IllegalArgumentException("cloneFromId must not be null");
        }

        // Get the source job
        ScheduledJobInfo sourceJob = jobSchedulerPort.getScheduledJobById(cloneFromId);
        if (sourceJob == null) {
            throw new IllegalArgumentException("Source job not found: " + cloneFromId);
        }

        log.infof("Cloning job %s (%s)", cloneFromId, sourceJob.getJobName());

        // Get the job definition for the source job's type
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(sourceJob.getJobType());
        if (jobDefOpt.isEmpty()) {
            throw new IllegalArgumentException("Job definition not found for type: " + sourceJob.getJobType());
        }

        JobDefinition jobDefinition = jobDefOpt.get();

        // Merge parameters: start with source parameters, then apply overrides
        Map<String, Object> mergedParameters = new HashMap<>(sourceJob.getParameters());
        if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
            mergedParameters.putAll(parameterOverrides);
            log.infof("Applied %s parameter override(s)", parameterOverrides.size());
        }

        // Create a new job name indicating it's a clone
        if (suffix == null || suffix.isBlank()) {
            suffix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        String newJobName = sourceJob.getJobName() + "-" + suffix;

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
