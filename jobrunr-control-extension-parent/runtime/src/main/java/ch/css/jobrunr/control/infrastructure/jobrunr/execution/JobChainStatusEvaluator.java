package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.JobAwaitingState;
import ch.css.jobrunr.control.domain.JobStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.AwaitingState;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Evaluates the overall status of a job chain by analyzing continuation jobs.
 * <p>
 * Simplified implementation that directly queries JobRunr storage and evaluates the chain
 * without intermediate data structures.
 * <p>
 * Job Chain Rules (from JobRunr Pro):
 * - continueWith(): Creates a continuation job that runs ONLY if parent SUCCEEDS
 * - onFailure(): Creates a continuation job that runs ONLY if parent FAILS
 * <p>
 * Status Logic:
 * - Chain is COMPLETE when all relevant leaf jobs have finished (SUCCEEDED, FAILED, or DELETED)
 * - Chain is IN_PROGRESS when any leaf job is still running (ENQUEUED, PROCESSING, PROCESSED)
 * - Chain SUCCEEDED when all executed leaf jobs succeeded
 * - Chain FAILED when any executed leaf job failed
 */
@ApplicationScoped
public class JobChainStatusEvaluator {

    private static final Logger LOG = Logger.getLogger(JobChainStatusEvaluator.class);

    private final StorageProvider storageProvider;
    private final JobStateMapper jobStateMapper;

    @Inject
    public JobChainStatusEvaluator(StorageProvider storageProvider, JobStateMapper jobStateMapper) {
        this.storageProvider = storageProvider;
        this.jobStateMapper = jobStateMapper;
    }

    /**
     * Evaluates the complete status of a job chain starting from a parent job.
     *
     * @param parentJobId     the UUID of the parent job
     * @param parentJobStatus the current status of the parent job
     * @return JobChainStatus containing completion state and overall status
     */
    public JobChainStatus evaluateChainStatus(UUID parentJobId, JobStatus parentJobStatus) {
        try {
            // Find direct continuation jobs
            List<Job> continuationJobs = findContinuationJobs(parentJobId);

            // If no continuation jobs exist, the chain status is just the parent status
            if (continuationJobs.isEmpty()) {
                return new JobChainStatus(isTerminalStatus(parentJobStatus), parentJobStatus);
            }

            // Find all leaf jobs recursively
            List<Job> leafJobs = findLeafJobs(continuationJobs);

            // Evaluate based on leaf jobs
            return evaluateLeafJobs(parentJobStatus, leafJobs);

        } catch (Exception e) {
            LOG.errorf(e, "Error evaluating chain status for parent job %s", parentJobId);
            return new JobChainStatus(isTerminalStatus(parentJobStatus), parentJobStatus);
        }
    }

    /**
     * Finds all direct continuation jobs for a parent job.
     */
    private List<Job> findContinuationJobs(UUID parentJobId) {
        var searchRequest = JobSearchRequestBuilder
                .aJobSearchRequest()
                .withAwaitingOn(parentJobId)
                .build();
        var amountRequest = new org.jobrunr.storage.navigation.AmountRequest("updatedAt:DESC", 100);
        return storageProvider.getJobList(searchRequest, amountRequest);
    }

    /**
     * Recursively finds all leaf jobs (jobs with no children) in the job tree.
     */
    private List<Job> findLeafJobs(List<Job> jobs) {
        return jobs.stream()
                .flatMap(job -> {
                    List<Job> children = findContinuationJobs(job.getId());
                    if (children.isEmpty()) {
                        // This is a leaf node
                        return java.util.stream.Stream.of(job);
                    } else {
                        // Recursively find leaf nodes
                        return findLeafJobs(children).stream();
                    }
                })
                .toList();
    }

    /**
     * Evaluates the status based on leaf continuation jobs.
     */
    private JobChainStatus evaluateLeafJobs(JobStatus parentJobStatus, List<Job> leafJobs) {
        // Filter leaf jobs that should execute based on parent status
        List<Job> relevantLeafJobs = leafJobs.stream()
                .filter(leaf -> shouldExecute(leaf, parentJobStatus))
                .toList();

        // If no relevant leaf jobs, chain is complete with parent status
        if (relevantLeafJobs.isEmpty()) {
            return new JobChainStatus(true, parentJobStatus);
        }

        // Check if all relevant leaf jobs are complete
        boolean allComplete = relevantLeafJobs.stream()
                .map(job -> jobStateMapper.mapJobState(job.getJobState()))
                .allMatch(this::isTerminalStatus);

        if (!allComplete) {
            return new JobChainStatus(false, JobStatus.PROCESSING);
        }

        // All relevant leaf jobs are complete - determine final status
        boolean anyFailed = relevantLeafJobs.stream()
                .map(job -> jobStateMapper.mapJobState(job.getJobState()))
                .anyMatch(status -> status == JobStatus.FAILED);

        JobStatus finalStatus = anyFailed ? JobStatus.FAILED : JobStatus.SUCCEEDED;
        return new JobChainStatus(true, finalStatus);
    }

    /**
     * Determines if a leaf job should execute based on the parent job's status.
     */
    private boolean shouldExecute(Job leafJob, JobStatus parentStatus) {
        Set<JobAwaitingState> awaitingStates = extractAwaitingStates(leafJob);

        if (awaitingStates.isEmpty()) {
            return true; // Defensive: assume it should execute
        }

        return awaitingStates.stream().anyMatch(state -> matchesParentStatus(state, parentStatus));
    }

    /**
     * Extracts the awaiting states from a job.
     * <p>
     * LIMITATION: JobRunr Pro's AwaitingState does not expose the continuation type (continueWith vs onFailure)
     * through its public API, and JobSearchRequestBuilder does not provide a withAwaitingOnStates() method
     * to filter by parent state triggers.
     * <p>
     * Conservative Approach: This method returns BOTH SUCCEEDED and FAILED states to ensure all continuation
     * types are considered. This may include jobs that won't execute (e.g., an onFailure continuation when
     * the parent succeeds), but ensures complete chain status evaluation.
     * <p>
     * Impact:
     * - Chain status evaluation is conservative (may show IN_PROGRESS for jobs that won't execute)
     * - More accurate than assuming only SUCCEEDED, which would miss onFailure() continuations
     * <p>
     * Alternative Solutions:
     * 1. Use reflection to access JobRunr internals (fragile, version-dependent)
     * 2. Request JobRunr Pro to expose continuation type in AwaitingState API
     * 3. Maintain external metadata about continuation types (requires code changes)
     *
     * @param job the job to analyze
     * @return set of awaiting states (currently returns both SUCCEEDED and FAILED for safety)
     */
    private Set<JobAwaitingState> extractAwaitingStates(Job job) {
        // Check current state or job history for AwaitingState
        if (job.getJobState() instanceof AwaitingState) {
            // Conservative approach: assume job could be triggered by either parent success or failure
            return Set.of(JobAwaitingState.SUCCEEDED, JobAwaitingState.FAILED);
        }

        // Check job history for AwaitingState
        var awaitingStateOpt = job.getJobStates().stream()
                .filter(s -> s instanceof AwaitingState)
                .findFirst();

        if (awaitingStateOpt.isPresent()) {
            // Conservative approach: assume job could be triggered by either parent success or failure
            return Set.of(JobAwaitingState.SUCCEEDED, JobAwaitingState.FAILED);
        }

        return Set.of();
    }

    /**
     * Checks if a JobAwaitingState matches the parent job's status.
     */
    private boolean matchesParentStatus(JobAwaitingState awaitingState, JobStatus parentStatus) {
        return switch (awaitingState) {
            case SUCCEEDED -> parentStatus == JobStatus.SUCCEEDED;
            case FAILED -> parentStatus == JobStatus.FAILED;
        };
    }

    /**
     * Checks if a job status is terminal (complete, won't change anymore).
     */
    private boolean isTerminalStatus(JobStatus status) {
        return switch (status) {
            case SUCCEEDED, FAILED, DELETED, NOT_APPLICABLE -> true;
            case ENQUEUED, PROCESSING, PROCESSED -> false;
        };
    }

    /**
     * Result of job chain status evaluation.
     *
     * @param isComplete    true if the entire chain has completed execution
     * @param overallStatus the overall status of the chain (SUCCEEDED, FAILED, or PROCESSING)
     */
    public record JobChainStatus(
            boolean isComplete,
            JobStatus overallStatus
    ) {
    }
}
