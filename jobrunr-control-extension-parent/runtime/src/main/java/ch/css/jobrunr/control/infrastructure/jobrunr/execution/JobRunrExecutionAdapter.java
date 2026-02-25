package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.infrastructure.jobrunr.ConfigurableJobSearchAdapter;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobResultAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.states.*;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JobRunr-based implementation of JobExecutionPort.
 * Uses JobRunr's StorageProvider for job queries.
 */
@ApplicationScoped
public class JobRunrExecutionAdapter implements JobExecutionPort {

    private static final Logger LOG = Logger.getLogger(JobRunrExecutionAdapter.class);

    private final StorageProvider storageProvider;
    private final ConfigurableJobSearchAdapter configurableJobSearchAdapter;
    private final JobChainStatusEvaluator jobChainStatusEvaluator;
    private final JobStateMapper jobStateMapper;
    private final ParameterSetLoaderPort parameterSetLoaderPort;

    @Inject
    public JobRunrExecutionAdapter(
            StorageProvider storageProvider,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            ConfigurableJobSearchAdapter configurableJobSearchAdapter,
            JobChainStatusEvaluator jobChainStatusEvaluator,
            JobStateMapper jobStateMapper,
            ParameterSetLoaderPort parameterSetLoaderPort
    ) {
        this.storageProvider = storageProvider;
        this.configurableJobSearchAdapter = configurableJobSearchAdapter;
        this.jobChainStatusEvaluator = jobChainStatusEvaluator;
        this.jobStateMapper = jobStateMapper;
        this.parameterSetLoaderPort = parameterSetLoaderPort;
    }

    @Override
    public List<JobExecutionInfo> getJobExecutions() {
        List<StateName> relevantStates = List.of(
                StateName.ENQUEUED,
                StateName.AWAITING,
                StateName.PROCESSING,
                StateName.PROCESSED,
                StateName.SUCCEEDED,
                StateName.FAILED
        );
        return configurableJobSearchAdapter.getConfigurableJob(relevantStates)
                .stream().map(j -> mapToJobExecutionInfo(j.jobDefinition().jobType(), j.job()))
                .toList();
    }

    @Override
    public Optional<JobExecutionInfo> getJobExecutionById(UUID jobId) {
        try {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            String jobType = job.getLabels().stream()
                    .filter(label -> label.startsWith("jobtype:"))
                    .map(label -> label.substring(8))
                    .findFirst()
                    .orElse(null);
            return Optional.of(mapToJobExecutionInfo(jobType, job));
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving job %s", jobId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<JobExecutionInfo> getJobChainExecutionById(UUID jobId) {
        try {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            String jobType = job.getLabels().stream()
                    .filter(label -> label.startsWith("jobtype:"))
                    .map(label -> label.substring(8))
                    .findFirst()
                    .orElse(null);

            JobExecutionInfo jobInfo = mapToJobExecutionInfo(jobType, job);

            // Evaluate the job chain status
            JobChainStatusEvaluator.JobChainStatus chainStatus =
                    jobChainStatusEvaluator.evaluateChainStatus(jobId, jobInfo.status());

            // Find result: parent job first, then direct continuation jobs
            String result = jobInfo.result();
            Integer resultCode = jobInfo.resultCode();
            if (result == null && resultCode == null) {
                org.jobrunr.jobs.Job resultJob = findResultJobInContinuationJobs(jobId);
                if (resultJob != null) {
                    result = extractResult(resultJob);
                    resultCode = extractResultCode(resultJob);
                }
            }

            return Optional.of(jobInfo.withStatus(chainStatus.overallStatus()).withResult(result, resultCode));
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving job chain %s", jobId);
            return Optional.empty();
        }
    }

    private JobExecutionInfo mapToJobExecutionInfo(String jobType, org.jobrunr.jobs.Job job) {
        JobStatus status = jobStateMapper.mapJobState(job.getJobState());
        Instant startedAt = extractStartedAt(job);
        Instant finishedAt = extractFinishedAt(job);
        BatchProgress batchProgress = extractBatchProgress(job);
        String jobName = job.getJobName();
        var parameters = parameterSetLoaderPort.loadParameters(job.getId());
        var metadata = job.getMetadata().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("jobRunr"))
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue
                ));

        String result = extractResult(job);
        Integer resultCode = extractResultCode(job);

        return new JobExecutionInfo(
                job.getId(),
                jobName,
                jobType,
                status,
                startedAt,
                finishedAt,
                batchProgress,
                parameters,
                metadata,
                result,
                resultCode
        );
    }


    private Instant extractStartedAt(org.jobrunr.jobs.Job job) {
        // Suche nach PROCESSING State in der Job-History
        return job.getJobStates().stream()
                .filter(ProcessingState.class::isInstance)
                .map(JobState::getCreatedAt)
                .findFirst()
                .orElse(null);
    }

    private Instant extractFinishedAt(org.jobrunr.jobs.Job job) {
        JobState state = job.getJobState();
        if (state instanceof SucceededState || state instanceof FailedState) {
            return state.getCreatedAt();
        }
        return null;
    }

    private String extractResult(org.jobrunr.jobs.Job job) {
        Object value = job.getMetadata().get(JobResultAdapter.RESULT_METADATA_KEY);
        return value != null ? value.toString() : null;
    }

    private Integer extractResultCode(org.jobrunr.jobs.Job job) {
        Object value = job.getMetadata().get(JobResultAdapter.RESULT_CODE_METADATA_KEY);
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * Returns the first direct continuation job (success/failure callback) that has a stored result.
     * This allows callback handlers to set a result that is surfaced on the parent job's status endpoint.
     */
    private org.jobrunr.jobs.Job findResultJobInContinuationJobs(UUID parentJobId) {
        try {
            var searchRequest = JobSearchRequestBuilder
                    .aJobSearchRequest()
                    .withAwaitingOn(parentJobId)
                    .build();
            var amountRequest = new AmountRequest("updatedAt:DESC", 100);
            return storageProvider.getJobList(searchRequest, amountRequest).stream()
                    .filter(j -> extractResult(j) != null || extractResultCode(j) != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to find result in continuation jobs for parent %s", parentJobId);
            return null;
        }
    }

    @SuppressWarnings("unused")
    private BatchProgress extractBatchProgress(org.jobrunr.jobs.Job job) {
        try {
            if (job.isBatchJob()) {
                BatchJob batchJob = (BatchJob) job;
                BatchJob.BatchJobStats batchJobStats = batchJob.getBatchJobStats();
                long totalJobs = batchJobStats.getTotalChildJobCount();
                long succeededJobs = batchJobStats.getSucceededChildJobCount();
                long failedJobs = batchJobStats.getFailedChildJobCount();
                return new BatchProgress(totalJobs, succeededJobs, failedJobs);
            }
        } catch (IllegalStateException e) {
            // Because batch job stats are only available when the batch job is started
            // Bug in JobRunr?
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage());
            }
        }
        return null;
    }
}
