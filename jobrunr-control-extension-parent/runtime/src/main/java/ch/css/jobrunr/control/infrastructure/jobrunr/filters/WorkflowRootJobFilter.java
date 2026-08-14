package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import ch.css.jobrunr.control.infrastructure.jobrunr.JobWorkflowResolver;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobClientFilter;
import org.jobrunr.jobs.states.AwaitingState;
import org.jobrunr.jobs.states.InitialState;
import org.jobrunr.utils.JobHolderContext;

import java.util.Optional;
import java.util.UUID;

/**
 * Stamps nested jobs with their canonical workflow root without consuming a JobRunr label.
 */
@ApplicationScoped
public class WorkflowRootJobFilter implements JobClientFilter {

    @Override
    public void onCreating(AbstractJob abstractJob) {
        if (!(abstractJob instanceof Job job)) {
            return;
        }

        if (JobHolderContext.hasJob()) {
            Job parentJob = JobHolderContext.getJob();
            UUID rootId = rootIdFromMetadata(parentJob).orElse(parentJob.getId());
            stamp(job, rootId);
            return;
        }

        // Continuations and BatchJob members configured by JobRunr have no creation owner in the
        // client-filter context. Leave them unstamped so JobWorkflowResolver can follow their
        // AwaitingState/parent relationship to the actual root without one storage lookup here
        // for every created worker.
        boolean isAwaitingJobWithoutCreationOwner = job.getJobStates().stream()
                .filter(InitialState.class::isInstance)
                .map(InitialState.class::cast)
                .findFirst()
                .filter(AwaitingState.class::isInstance)
                .isPresent();
        if (!isAwaitingJobWithoutCreationOwner) {
            stamp(job, job.getId());
        }
    }

    private Optional<UUID> rootIdFromMetadata(Job job) {
        Object value = job.getMetadata().get(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY);
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (value instanceof String stringValue) {
            try {
                return Optional.of(UUID.fromString(stringValue));
            } catch (IllegalArgumentException ignored) {
                // The resolver will recover through graph relationships if metadata is malformed.
            }
        }
        return Optional.empty();
    }

    private void stamp(Job job, UUID rootId) {
        job.getMetadata().put(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, rootId.toString());
    }
}
