package ch.css.jobrunr.control.infrastructure.jobrunr;

import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.AwaitingState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.StateName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobWorkflowResolverTest {

    private static final JobDetails JOB_DETAILS = new JobDetails("test.Handler", "run", null, List.of());

    @Test
    void resolvesRootThroughCreationParents() {
        UUID rootId = UUID.randomUUID();
        UUID innerBatchId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        Job root = batchJob(rootId, new EnqueuedState());
        Job innerBatch = batchJob(innerBatchId,
                new AwaitingState(rootId, StateName.PROCESSED, null, rootId));
        Job worker = job(workerId,
                new AwaitingState(innerBatchId, StateName.PROCESSED, null, innerBatchId));
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(root, innerBatch, worker);

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveRootId(workerId)).isEqualTo(rootId);
    }

    @Test
    void stampedRootAvoidsRelationTraversal() {
        UUID rootId = UUID.randomUUID();
        Job worker = job(UUID.randomUUID(), new EnqueuedState());
        worker.getMetadata().put(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, rootId.toString());
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(worker);

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveRootId(worker.getId())).isEqualTo(rootId);
        assertThat(lookup.loadedJobIds).containsExactly(worker.getId());
    }

    @Test
    void legacyContinuationFallsBackToAwaitedJob() {
        UUID rootId = UUID.randomUUID();
        Job root = batchJob(rootId, new EnqueuedState());
        Job continuation = job(UUID.randomUUID(),
                new AwaitingState(rootId, StateName.SUCCEEDED, null, null));
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(root, continuation);

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveRootId(continuation.getId())).isEqualTo(rootId);
    }

    @Test
    void resolvesNestedBatchesWorkersAndSiblingContinuationsWithoutExpandingWorkers() {
        Job root = batchJob(UUID.randomUUID(), new EnqueuedState());
        Job innerBatch = batchJob(UUID.randomUUID(), new EnqueuedState());
        Job workerA = job(UUID.randomUUID(), new EnqueuedState());
        Job workerB = job(UUID.randomUUID(), new EnqueuedState());
        Job success = job(UUID.randomUUID(), new EnqueuedState());
        Job failure = job(UUID.randomUUID(), new EnqueuedState());
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(root, innerBatch, workerA, workerB, success, failure);
        lookup.children.put(root.getId(), List.of(innerBatch, success, failure));
        lookup.children.put(innerBatch.getId(), List.of(workerA, workerB));
        lookup.continuations.put(root.getId(), List.of(innerBatch));
        lookup.continuations.put(innerBatch.getId(), List.of(success, failure));

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveWorkflow(root.getId()))
                .extracting(Job::getId)
                .containsExactlyInAnyOrder(
                        root.getId(), innerBatch.getId(), workerA.getId(), workerB.getId(),
                        success.getId(), failure.getId());
        assertThat(lookup.continuationLookups).doesNotContain(workerA.getId(), workerB.getId());
    }

    @Test
    void expandsLegacyAwaitingChainAtArbitraryDepth() {
        Job root = batchJob(UUID.randomUUID(), new EnqueuedState());
        Job continuationA = job(UUID.randomUUID(), new EnqueuedState());
        Job continuationB = job(UUID.randomUUID(), new EnqueuedState());
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(root, continuationA, continuationB);
        lookup.continuations.put(root.getId(), List.of(continuationA));
        lookup.continuations.put(continuationA.getId(), List.of(continuationB));

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveWorkflow(root.getId()))
                .extracting(Job::getId)
                .containsExactly(root.getId(), continuationA.getId(), continuationB.getId());
    }

    private static Job batchJob(UUID id, org.jobrunr.jobs.states.JobState initialState) {
        return new BatchJob(id, JOB_DETAILS, initialState);
    }

    private static Job job(UUID id, org.jobrunr.jobs.states.JobState initialState) {
        return new Job(id, JOB_DETAILS, initialState);
    }

    private static final class FakeWorkflowJobLookup implements JobWorkflowResolver.WorkflowJobLookup {
        private final Map<UUID, Job> jobs = new HashMap<>();
        private final Map<UUID, List<Job>> children = new HashMap<>();
        private final Map<UUID, List<Job>> continuations = new HashMap<>();
        private final List<UUID> loadedJobIds = new ArrayList<>();
        private final Set<UUID> continuationLookups = new HashSet<>();

        private FakeWorkflowJobLookup(Job... jobs) {
            for (Job job : jobs) {
                this.jobs.put(job.getId(), job);
            }
        }

        @Override
        public Job getJobById(UUID jobId) {
            loadedJobIds.add(jobId);
            return jobs.get(jobId);
        }

        @Override
        public List<Job> findChildren(UUID parentJobId) {
            return children.getOrDefault(parentJobId, List.of());
        }

        @Override
        public List<Job> findContinuations(UUID awaitedJobId) {
            continuationLookups.add(awaitedJobId);
            return continuations.getOrDefault(awaitedJobId, List.of());
        }
    }
}
