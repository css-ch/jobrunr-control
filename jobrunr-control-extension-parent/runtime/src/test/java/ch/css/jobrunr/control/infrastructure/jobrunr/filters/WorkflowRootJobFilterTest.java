package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import ch.css.jobrunr.control.infrastructure.jobrunr.JobWorkflowResolver;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.AwaitingState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.utils.JobHolderContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRootJobFilterTest {

    private static final JobDetails JOB_DETAILS = new JobDetails("test.Handler", "run", null, List.of());
    private final WorkflowRootJobFilter filter = new WorkflowRootJobFilter();

    @AfterEach
    void clearJobContext() {
        JobHolderContext.clear();
    }

    @Test
    void stampsTopLevelJobWithItsOwnId() {
        Job root = new Job(UUID.randomUUID(), JOB_DETAILS, new EnqueuedState());

        filter.onCreating(root);

        assertThat(root.getMetadata())
                .containsEntry(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, root.getId().toString());
    }

    @Test
    void inheritsStampedRootFromCurrentParent() {
        UUID rootId = UUID.randomUUID();
        Job parent = new Job(UUID.randomUUID(), JOB_DETAILS, new EnqueuedState());
        parent.getMetadata().put(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, rootId.toString());
        JobHolderContext.setJob(parent);
        Job child = new Job(UUID.randomUUID(), JOB_DETAILS, new EnqueuedState());

        filter.onCreating(child);

        assertThat(child.getMetadata())
                .containsEntry(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, rootId.toString());
    }

    @Test
    void leavesUnownedContinuationUnstampedForResolverFallback() {
        Job continuation = new Job(UUID.randomUUID(), JOB_DETAILS,
                new AwaitingState(UUID.randomUUID(), StateName.SUCCEEDED, null, null));

        filter.onCreating(continuation);

        assertThat(continuation.getMetadata())
                .doesNotContainKey(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY);
    }
}
