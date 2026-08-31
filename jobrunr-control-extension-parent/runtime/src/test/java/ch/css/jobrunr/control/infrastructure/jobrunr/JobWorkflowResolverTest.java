package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.BatchProgress;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.AwaitingBatchJobState;
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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        Job innerBatch = batchJob(UUID.randomUUID(), new AwaitingBatchJobState(root.getId()));
        Job workerA = job(UUID.randomUUID(), new AwaitingBatchJobState(innerBatch.getId()));
        Job workerB = job(UUID.randomUUID(), new AwaitingBatchJobState(innerBatch.getId()));
        Job success = job(UUID.randomUUID(), new AwaitingState(innerBatch.getId(), StateName.SUCCEEDED));
        Job failure = job(UUID.randomUUID(), new AwaitingState(innerBatch.getId(), StateName.FAILED));
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
        assertThat(resolver.resolveProcessingJobs(root.getId()))
                .extracting(Job::getId)
                .containsExactlyInAnyOrder(workerA.getId(), workerB.getId());
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

    @Test
    void resolvesProcessingJobProgressThroughDomainPort() {
        Job root = batchJob(UUID.randomUUID(), new EnqueuedState());
        Job succeededWorker = processingJob(root.getId(), StateName.SUCCEEDED);
        Job failedWorker = processingJob(root.getId(), StateName.FAILED);
        Job processingWorker = processingJob(root.getId(), StateName.PROCESSING);
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(
                root, succeededWorker, failedWorker, processingWorker);
        lookup.children.put(root.getId(), List.of(succeededWorker, failedWorker, processingWorker));

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveProcessingJobProgress(root.getId()))
                .isEqualTo(new BatchProgress(3, 1, 1));
    }

    @Test
    void aggregatesProcessingProgressRecursivelyAcrossNestedBatchChildren() {
        Job root = batchJob(UUID.randomUUID(), new EnqueuedState());
        Job innerBatch = batchJob(UUID.randomUUID(), new AwaitingBatchJobState(root.getId()));
        Job workerA = processingJob(innerBatch.getId(), StateName.SUCCEEDED);
        Job workerB = processingJob(innerBatch.getId(), StateName.FAILED);
        FakeWorkflowJobLookup lookup = new FakeWorkflowJobLookup(root, innerBatch, workerA, workerB);
        lookup.children.put(root.getId(), List.of(innerBatch));
        lookup.children.put(innerBatch.getId(), List.of(workerA, workerB));

        JobWorkflowResolver resolver = new JobWorkflowResolver(lookup);

        assertThat(resolver.resolveProcessingJobProgress(root.getId()))
                .isEqualTo(new BatchProgress(2, 1, 1));
    }

    private static Job batchJob(UUID id, org.jobrunr.jobs.states.JobState initialState) {
        return new BatchJob(id, JOB_DETAILS, initialState);
    }

    private static Job job(UUID id, org.jobrunr.jobs.states.JobState initialState) {
        return new Job(id, JOB_DETAILS, initialState);
    }

    private static Job processingJob(UUID batchId, StateName state) {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(UUID.randomUUID());
        when(job.getJobStates()).thenReturn(List.of(new AwaitingBatchJobState(batchId)));
        when(job.getState()).thenReturn(state);
        return job;
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

        @Override
        public List<Job> findBatchChildren(UUID parentJobId) {
            return findChildren(parentJobId).stream()
                    .filter(Job::isBatchJob)
                    .toList();
        }

        @Override
        public long countOrdinaryChildren(UUID parentJobId) {
            return findChildren(parentJobId).stream()
                    .filter(not(Job::isBatchJob))
                    .count();
        }

        @Override
        public long countOrdinaryChildren(UUID parentJobId, StateName state) {
            return findChildren(parentJobId).stream()
                    .filter(not(Job::isBatchJob))
                    .filter(job -> job.getState() == state)
                    .count();
        }
    }
}
