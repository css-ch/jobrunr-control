package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobDetailPage;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.details.JobDetailsProviderRegistry;
import ch.css.jobrunr.control.domain.details.JobMessageProvider;
import ch.css.jobrunr.control.domain.details.JobRecapProvider;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobWorkflowResolver;
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
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Inject
    public ParameterCleanupJobFilter(
            ParameterStoragePort parameterStoragePort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobDetailsProviderRegistry jobDetailsProviderRegistry) {
        this.parameterStoragePort = parameterStoragePort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
    }

    public ParameterCleanupJobFilter(
            ParameterStoragePort parameterStoragePort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this(parameterStoragePort, jobDefinitionDiscoveryService, null);
    }

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        if (newState.getName() == StateName.DELETED) {
            cleanupParametersIfExists(job);
            cleanupJobMessagesAndRecapIfRootJob(job);
        }
    }

    /**
     * Checks if the job type uses external parameters and deletes them if so.
     * The parameter set ID is always equal to the job ID.
     */
    private void cleanupParametersIfExists(Job job) {
        try {
            String handlerClassName = job.getJobDetails().getClassName();

            boolean usesExternalParameters = jobDefinitionDiscoveryService
                    .findJobByHandlerClassName(handlerClassName)
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

    private void cleanupJobMessagesAndRecapIfRootJob(Job job) {
        try {
            UUID rootJobId = rootIdFromMetadata(job);
            if (rootJobId != null && !job.getId().equals(rootJobId)) {
                return;
            }

            String handlerClassName = job.getJobDetails().getClassName();
            JobDefinition jobDefinition = jobDefinitionDiscoveryService
                    .findJobByHandlerClassName(handlerClassName)
                    .filter(definition -> isCanonicalRootFallback(job, definition, rootJobId))
                    .orElse(null);
            if (jobDefinition == null || jobDetailsProviderRegistry == null) {
                return;
            }

            JobDetailPage detailPage = jobDefinition.jobDetailPage();
            String messageProviderKey = detailPage == null ? null : detailPage.messageProviderKey();
            String recapProviderKey = detailPage == null ? null : detailPage.recapProviderKey();
            JobMessageProvider messageProvider = jobDetailsProviderRegistry.getMessageProvider(messageProviderKey);
            JobRecapProvider recapProvider = jobDetailsProviderRegistry.getRecapProvider(recapProviderKey);

            if (messageProvider == recapProvider) {
                messageProvider.deleteByRootJobId(job.getId());
            } else {
                messageProvider.deleteByRootJobId(job.getId());
                recapProvider.deleteByRootJobId(job.getId());
            }
            LOG.infof("Deleted persisted job details for workflow root %s", job.getId());
        } catch (Exception e) {
            LOG.warnf(e, "Failed to cleanup persisted job details for job %s", job.getId());
        }
    }

    private UUID rootIdFromMetadata(Job job) {
        Object value = job.getMetadata().get(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String stringValue) {
            try {
                return UUID.fromString(stringValue);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isCanonicalRootFallback(Job job, JobDefinition jobDefinition, UUID rootJobId) {
        if (rootJobId != null) {
            return job.getId().equals(rootJobId);
        }
        return jobDefinition.handlerClassName().equals(job.getJobDetails().getClassName())
                && job.getJobDetails().getJobParameters().stream()
                .anyMatch(parameter -> jobDefinition.jobRequestTypeName().equals(parameter.getClassName())
                        || jobDefinition.jobRequestTypeName().equals(parameter.getActualClassName()));
    }
}
