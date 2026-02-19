package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;

import java.util.UUID;

/**
 * JobRunr filter that cleans up external parameter sets when jobs are deleted.
 * <p>
 * This filter intercepts job state changes and deletes the associated parameter set
 * when a job transitions to DELETED state. Whether a job uses external parameters is
 * determined by its job type definition. The parameter set ID is always equal to the job ID.
 */
@ApplicationScoped
public class ParameterCleanupJobFilter implements ApplyStateFilter, JobServerFilter {

    private static final Logger LOG = Logger.getLogger(ParameterCleanupJobFilter.class);

    private final ParameterStoragePort parameterStoragePort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public ParameterCleanupJobFilter(
            ParameterStoragePort parameterStoragePort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.parameterStoragePort = parameterStoragePort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        if (newState.getName() == StateName.DELETED) {
            cleanupParametersIfExists(job);
        }
    }

    /**
     * Checks if the job type uses external parameters and deletes them if so.
     * The parameter set ID is always equal to the job ID.
     */
    private void cleanupParametersIfExists(Job job) {
        try {
            String handlerClassName = job.getJobDetails().getClassName();
            String simpleClassName = handlerClassName.substring(handlerClassName.lastIndexOf('.') + 1);

            boolean usesExternalParameters = jobDefinitionDiscoveryService
                    .findJobByType(simpleClassName)
                    .map(JobDefinition::usesExternalParameters)
                    .orElse(false);

            if (usesExternalParameters) {
                UUID jobId = job.getId();
                LOG.debugf("Deleting external parameters for deleted job %s", jobId);
                parameterStoragePort.deleteById(jobId);
                LOG.infof("Deleted parameter set %s for job %s", jobId, jobId);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to cleanup parameters for job %s", job.getId());
            // Don't throw - we don't want to prevent job deletion
        }
    }
}
