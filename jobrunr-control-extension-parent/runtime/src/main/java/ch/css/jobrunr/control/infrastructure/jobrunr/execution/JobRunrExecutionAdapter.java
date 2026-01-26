package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobParameterExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.*;
import org.jobrunr.storage.JobSearchRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JobRunr-based implementation of JobExecutionPort.
 * Uses JobRunr's StorageProvider for job queries.
 */
@ApplicationScoped
public class JobRunrExecutionAdapter implements JobExecutionPort {

    private static final Logger log = LoggerFactory.getLogger(JobRunrExecutionAdapter.class);

    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public JobRunrExecutionAdapter(
            StorageProvider storageProvider,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    @Override
    public List<JobExecutionInfo> getJobExecutions() {
        List<JobExecutionInfo> jobExecutionInfos = new ArrayList<>();
        try {
            // Define states to query
            StateName[] statesToQuery = {
                    StateName.ENQUEUED,
                    StateName.AWAITING,
                    StateName.PROCESSING,
                    StateName.PROCESSED,
                    StateName.SUCCEEDED,
                    StateName.FAILED
            };

            AmountRequest amountRequest = new AmountRequest("updatedAt:DESC", 1000);

            for (StateName state : statesToQuery) {
                for (JobDefinition jobDefinition : jobDefinitionDiscoveryService.getAllJobDefinitions()) {
                    try {
                        JobSearchRequest searchRequest;
                        if (jobDefinition.isBatchJob()) {
                            searchRequest = createSearchRequestForStateAndJobTypeForBatch(state, jobDefinition.jobType());
                        } else {
                            searchRequest = createSearchRequestForStateAndJobType(state, jobDefinition.jobType());
                        }

                        List<Job> jobList = storageProvider.getJobList(searchRequest, amountRequest);
                        for (Job job : jobList) {
                            JobExecutionInfo jobExecutionInfo = mapToJobExecutionInfo(jobDefinition.jobType(), job);
                            if (!isChildJobOfBatch(job, jobDefinition)) { // Check is necessary because JobRunr copies labels to child jobs
                                jobExecutionInfos.add(jobExecutionInfo);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Fehler beim Abrufen von Jobs im Status {} und Typ {}: {}", state, jobDefinition.jobType(), e.getMessage());
                    }
                }
            }
            return jobExecutionInfos;
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Job-Ausführungen", e);
            throw new JobExecutionException("Fehler beim Abrufen der Job-Ausführungen", e);
        }
    }

    private boolean isChildJobOfBatch(Job job, JobDefinition jobDefinition) {
        return jobDefinition.isBatchJob() && getParentJob(job) != null;
    }

    private UUID getParentJob(Job job) {
        return job.getJobStatesOfType(EnqueuedState.class).findFirst().map(AbstractInitialJobState::getParentJobId).orElse(null);
    }

    private static JobSearchRequest createSearchRequestForStateAndJobType(StateName state, String jobType) {
        return new JobSearchRequest(
                state, // state
                null,          // priority
                null,          // jobId
                null,          // jobIdGreaterThan
                null,          // jobIds
                null,          // jobName
                null,          // jobSignature
                null,          // jobExceptionType
                null,          // jobFingerprint
                "jobtype:" + jobType,          // label
                null,          // serverTag
                null,          // mutex
                null,          // recurringJobId
                null,          // recurringJobIds
                null,          // awaitingOn
                null,          // parentId
                null,          // rateLimiter
                null,          // onlyBatchJobs
                null,          // createdAtFrom
                null,          // createdAtTo
                null,          // updatedAtFrom
                null,          // updatedAtTo
                null,          // scheduledAtFrom
                null,          // scheduledAtTo
                null           // deleteAtTo
        );
    }

    private static JobSearchRequest createSearchRequestForStateAndJobTypeForBatch(StateName state, String jobType) {
        return new JobSearchRequest(
                state, // state
                null,          // priority
                null,          // jobId
                null,          // jobIdGreaterThan
                null,          // jobIds
                null,          // jobName
                null,          // jobSignature
                null,          // jobExceptionType
                null,          // jobFingerprint
                "jobtype:" + jobType,          // label
                null,          // serverTag
                null,          // mutex
                null,          // recurringJobId
                null,          // recurringJobIds
                null,          // awaitingOn
                null,          // parentId
                null,          // rateLimiter
                true,          // onlyBatchJobs
                null,          // createdAtFrom
                null,          // createdAtTo
                null,          // updatedAtFrom
                null,          // updatedAtTo
                null,          // scheduledAtFrom
                null,          // scheduledAtTo
                null           // deleteAtTo
        );
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
            log.error("Fehler beim Abrufen von Job {}", jobId, e);
            return Optional.empty();
        }
    }

    private JobExecutionInfo mapToJobExecutionInfo(String jobType, org.jobrunr.jobs.Job job) {
        JobStatus status = mapJobState(job.getJobState());
        Instant startedAt = extractStartedAt(job);
        Instant finishedAt = extractFinishedAt(job);
        BatchProgress batchProgress = extractBatchProgress(job);
        String jobName = job.getJobName();
        var parameters = JobParameterExtractor.extractParameters(job);

        return new JobExecutionInfo(
                job.getId(),
                jobName,
                jobType,
                status,
                startedAt,
                finishedAt,
                batchProgress,
                parameters
        );
    }

    private JobStatus mapJobState(JobState jobState) {
        return switch (jobState) {
            case EnqueuedState s -> JobStatus.ENQUEUED;
            case ScheduledState s -> JobStatus.ENQUEUED;
            case AwaitingState s -> JobStatus.ENQUEUED;
            case ProcessingState s -> JobStatus.PROCESSING;
            case ProcessedState s -> JobStatus.PROCESSED;
            case SucceededState s -> JobStatus.SUCCEEDED;
            case FailedState s -> JobStatus.FAILED;
            case null, default -> {
                log.warn("Unexpected job state: {}. Defaulting to ENQUEUED.", jobState);
                yield JobStatus.ENQUEUED;
            }
        };
    }

    private Instant extractStartedAt(org.jobrunr.jobs.Job job) {
        // Suche nach PROCESSING State in der Job-History
        return job.getJobStates().stream()
                .filter(s -> s instanceof ProcessingState)
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
            log.debug(e.getMessage());
        }
        return null;
    }


    /**
     * Exception for job execution errors.
     */
    public static class JobExecutionException extends RuntimeException {
        public JobExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
