package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobWorkflowPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.AwaitingBatchJobState;
import org.jobrunr.jobs.states.AwaitingState;
import org.jobrunr.jobs.states.InitialState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves one logical JobRunr Control execution across nested batch and continuation jobs.
 * Parent edges represent workflow ownership, while awaiting edges represent execution dependencies.
 */
@ApplicationScoped
public class JobWorkflowResolver implements JobWorkflowPort {

    public static final String WORKFLOW_ROOT_ID_METADATA_KEY = "jobrunr-control-workflow-root-id";

    private static final Logger LOG = Logger.getLogger(JobWorkflowResolver.class);
    private static final AmountRequest ALL_RELATED_JOBS = AmountRequest.fromString("limit=1000000");

    private final WorkflowJobLookup jobLookup;

    @Inject
    public JobWorkflowResolver(StorageProvider storageProvider) {
        this(new StorageWorkflowJobLookup(storageProvider));
    }

    JobWorkflowResolver(WorkflowJobLookup jobLookup) {
        this.jobLookup = jobLookup;
    }

    /**
     * Resolves the canonical root for the currently executing job. Workflow roots and jobs created
     * from an active handler normally carry precomputed root metadata. JobRunr-created batch
     * members and legacy jobs use relation traversal as the fallback.
     */
    public UUID resolveRootIdFromContext() {
        var jobContext = ThreadLocalJobContext.getJobContext();
        UUID currentJobId = jobContext.getJobId();
        if (currentJobId == null) {
            throw new IllegalStateException("Cannot resolve workflow root outside a JobRunr job context");
        }
        return parseRootId(jobContext.getMetadata()).orElseGet(() -> resolveRootId(currentJobId));
    }

    /**
     * Walks creation-parent relationships towards the canonical root. For legacy continuations
     * without a creation parent, the awaited job is used as the fallback predecessor.
     */
    public UUID resolveRootId(UUID jobId) {
        UUID currentJobId = jobId;
        Set<UUID> visited = new HashSet<>();

        while (visited.add(currentJobId)) {
            Job currentJob = jobLookup.getJobById(currentJobId);
            Optional<UUID> stampedRootId = parseRootId(currentJob.getMetadata());
            if (stampedRootId.isPresent()) {
                return stampedRootId.get();
            }

            Optional<InitialState> creationState = firstInitialState(currentJob);
            Optional<UUID> parentJobId = creationState.map(InitialState::getParentJobId);
            if (parentJobId.isPresent()) {
                currentJobId = parentJobId.get();
                continue;
            }

            Optional<UUID> awaitedJobId = creationState
                    .filter(AwaitingState.class::isInstance)
                    .map(AwaitingState.class::cast)
                    .map(AwaitingState::getWaitingOnJob);
            if (awaitedJobId.isPresent()) {
                currentJobId = awaitedJobId.get();
                continue;
            }
            return currentJobId;
        }

        LOG.warnf("Cycle detected while resolving workflow root for job %s", jobId);
        return jobId;
    }

    /**
     * Returns the complete supported workflow rooted at {@code rootJobId}, including the root.
     *
     * Batch nodes are expanded through parent edges. Awaiting edges are expanded for roots, batch
     * nodes, and legacy continuation chains. Normal batch workers are treated as leaves: jobs that
     * create more work only while a normal worker is executing are not covered by the outer JobRunr
     * batch lifecycle and therefore cannot provide the promised root completion semantics anyway.
     */
    public List<Job> resolveWorkflow(UUID rootJobId) {
        Job rootJob = jobLookup.getJobById(rootJobId);
        Map<UUID, Job> jobsById = new LinkedHashMap<>();
        Set<UUID> expandedBatchParents = new HashSet<>();
        Set<UUID> expandedAwaitingJobs = new HashSet<>();
        ArrayDeque<TraversalTarget> pending = new ArrayDeque<>();
        pending.add(new TraversalTarget(rootJob, true));

        while (!pending.isEmpty()) {
            TraversalTarget target = pending.removeFirst();
            Job job = target.job();
            jobsById.putIfAbsent(job.getId(), job);

            if (job.isBatchJob() && expandedBatchParents.add(job.getId())) {
                findChildren(job.getId()).forEach(child ->
                        pending.addLast(new TraversalTarget(child, child.isBatchJob())));
            }

            if (target.expandAwaiting() && expandedAwaitingJobs.add(job.getId())) {
                findContinuations(job.getId()).forEach(continuation ->
                        pending.addLast(new TraversalTarget(continuation, true)));
            }
        }
        return List.copyOf(jobsById.values());
    }

    /**
     * Returns the jobs that perform the actual batch work. Batch wrappers and ordinary
     * success/failure continuations are lifecycle nodes and therefore do not contribute to the
     * worker success rate shown on the detail page.
     */
    public List<Job> resolveProcessingJobs(UUID rootJobId) {
        return resolveWorkflow(rootJobId).stream()
                .filter(job -> !job.getId().equals(rootJobId))
                .filter(job -> !job.isBatchJob())
                .filter(this::isBatchWorker)
                .toList();
    }

    @Override
    public BatchProgress resolveProcessingJobProgress(UUID rootJobId) {
        Job rootJob = jobLookup.getJobById(rootJobId);
        if (!rootJob.isBatchJob()) {
            throw new IllegalStateException("Job with ID " + rootJobId + " is not a batch job");
        }

        return aggregateProcessingProgress(rootJobId);
    }

    /**
     * Recursively aggregates processing-worker progress for {@code jobId} and its nested batches.
     */
    BatchProgress aggregateProcessingProgress(UUID jobId) {

        List<Job> batchChildren = jobLookup.findBatchChildren(jobId);

        BatchProgress progress = new BatchProgress(
                jobLookup.countOrdinaryChildren(jobId),
                jobLookup.countOrdinaryChildren(jobId, StateName.SUCCEEDED),
                jobLookup.countOrdinaryChildren(jobId, StateName.FAILED)
        );

        for (Job nestedBatch : batchChildren) {
            progress = progress.plus(aggregateProcessingProgress(nestedBatch.getId()));
        }

        return progress;
    }

    public Optional<UUID> parseRootId(Map<String, Object> metadata) {
        if (metadata == null) {
            return Optional.empty();
        }
        Object value = metadata.get(WORKFLOW_ROOT_ID_METADATA_KEY);
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Optional.of(UUID.fromString(stringValue));
            } catch (IllegalArgumentException e) {
                LOG.warnf("Ignoring invalid workflow root metadata value '%s'", stringValue);
            }
        }
        return Optional.empty();
    }

    private Optional<InitialState> firstInitialState(Job job) {
        return job.getJobStates().stream()
                .filter(InitialState.class::isInstance)
                .map(InitialState.class::cast)
                .findFirst();
    }

    private boolean isBatchWorker(Job job) {
        return firstInitialState(job)
                .map(state -> !(state instanceof AwaitingState)
                        || state instanceof AwaitingBatchJobState)
                .orElse(true);
    }

    private List<Job> findChildren(UUID parentJobId) {
        return jobLookup.findChildren(parentJobId);
    }

    private List<Job> findContinuations(UUID awaitedJobId) {
        return jobLookup.findContinuations(awaitedJobId);
    }

    private record TraversalTarget(Job job, boolean expandAwaiting) {
    }

    interface WorkflowJobLookup {
        Job getJobById(UUID jobId);

        List<Job> findChildren(UUID parentJobId);

        List<Job> findContinuations(UUID awaitedJobId);

        /**
         * Returns the direct children of {@code parentJobId} that are themselves batch jobs (i.e. nested batches).
         * This set is expected to be small (bounded by nesting fan-out, not by subjob count),
         * so materializing it is cheap even for very large batches.
         */
        List<Job> findBatchChildren(UUID parentJobId);

        /**
         * Counts all direct children of {@code parentJobId}, that are <em>not</em> batch jobs.
         */
        long countOrdinaryChildren(UUID parentJobId);

        /**
         * Counts the direct children of {@code parentJobId},
         * that are <em>not</em> batch jobs and are in the given {@code state}.
         */
        long countOrdinaryChildren(UUID parentJobId, StateName state);
    }

    private static final class StorageWorkflowJobLookup implements WorkflowJobLookup {
        private final StorageProvider storageProvider;

        private StorageWorkflowJobLookup(StorageProvider storageProvider) {
            this.storageProvider = storageProvider;
        }

        @Override
        public Job getJobById(UUID jobId) {
            return storageProvider.getJobById(jobId);
        }

        @Override
        public List<Job> findChildren(UUID parentJobId) {
            return new ArrayList<>(storageProvider.getJobList(
                    JobSearchRequestBuilder.aJobSearchRequest()
                            .withParentId(parentJobId)
                            .build(),
                    ALL_RELATED_JOBS));
        }

        @Override
        public List<Job> findContinuations(UUID awaitedJobId) {
            return new ArrayList<>(storageProvider.getJobList(
                    JobSearchRequestBuilder.aJobSearchRequest()
                            .withAwaitingOn(awaitedJobId)
                            .build(),
                    ALL_RELATED_JOBS));
        }

        @Override
        public List<Job> findBatchChildren(UUID parentJobId) {
            return new ArrayList<>(storageProvider.getJobList(
                    JobSearchRequestBuilder.aJobSearchRequest()
                            .withParentId(parentJobId)
                            .withOnlyBatchJobs(true)
                            .build(),
                    ALL_RELATED_JOBS));
        }

        @Override
        public long countOrdinaryChildren(UUID parentJobId) {
            return storageProvider.countJobs(
                    JobSearchRequestBuilder.aJobSearchRequest()
                            // the awaitingOn condition filters out success/failure continuations
                            .withAwaitingOn(parentJobId)
                            .withParentId(parentJobId)
                            .withOnlyBatchJobs(false)
                            .build());
        }

        @Override
        public long countOrdinaryChildren(UUID parentJobId, StateName state) {
            return storageProvider.countJobs(
                    JobSearchRequestBuilder.aJobSearchRequest()
                            // the awaitingOn condition filters out success/failure continuations
                            .withAwaitingOn(parentJobId)
                            .withParentId(parentJobId)
                            .withOnlyBatchJobs(false)
                            .withStateName(state)
                            .build());
        }
    }
}
