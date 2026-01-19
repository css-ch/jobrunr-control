package ch.css.jobrunr.control.infrastructure.execution;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.infrastructure.JobParameterExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.*;
import org.jobrunr.storage.JobSearchRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jspecify.annotations.NonNull;
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
    private final String dashboardUrl;

    @Inject
    public JobRunrExecutionAdapter(
            StorageProvider storageProvider,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            @ConfigProperty(name = "jobrunr.dashboard.url", defaultValue = "http://localhost:8000") String dashboardUrl) {
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.dashboardUrl = dashboardUrl;
    }

    @Override
    public List<JobExecutionInfo> getJobExecutions() {
        List<JobExecutionInfo> jobExecutionInfos = new ArrayList<>();
        try {
            // Define states to query
            StateName[] statesToQuery = {
                    StateName.PROCESSING,
                    StateName.PROCESSED,
                    StateName.SUCCEEDED,
                    StateName.FAILED
            };

            AmountRequest amountRequest = new AmountRequest("updatedAt:DESC", 1000);

            for (StateName state : statesToQuery) {
                for (JobDefinition jobDefinition : jobDefinitionDiscoveryService.getAllJobDefinitions()) {
                    try {
                        JobSearchRequest searchRequest = createSearchRequestForStateAndJobType(state, jobDefinition.type());
                        List<Job> jobList = storageProvider.getJobList(searchRequest, amountRequest);
                        for (Job job : jobList) {
                            List<Job> childJobs = storageProvider.getJobList(createSearchRequestForParentId(job.getId()), amountRequest);
                            if (childJobs.isEmpty()) {
                                jobExecutionInfos.add(mapToJobExecutionInfo(jobDefinition.type(), job));
                            } else {
                                jobExecutionInfos.add(mapToJobExecutionInfo(jobDefinition.type(), job));
                                for (Job childJob : childJobs) {
                                    jobExecutionInfos.add(mapToJobExecutionInfo(jobDefinition, job, childJob));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Fehler beim Abrufen von Jobs im Status {} und Typ {}: {}", state, jobDefinition.type(), e.getMessage());
                    }
                }
            }

            // Apply pagination to the combined result
            // Note: Jobs are already filtered by "dashboard:visible" label in JobSearchRequest
            return jobExecutionInfos;
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Job-Ausführungen", e);
            throw new JobExecutionException("Fehler beim Abrufen der Job-Ausführungen", e);
        }
    }

    private static @NonNull JobSearchRequest createSearchRequestForStateAndJobType(StateName state, String jobType) {
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

    private static @NonNull JobSearchRequest createSearchRequestForParentId(UUID parentId) {
        return new JobSearchRequest(
                (StateName) null, // state (Cast for overload resolution)
                null,          // priority
                null,          // jobId
                null,          // jobIdGreaterThan
                null,          // jobIds
                null,          // jobName
                null,          // jobSignature
                null,          // jobExceptionType
                null,          // jobFingerprint
                null,          // label
                null,          // serverTag
                null,          // mutex
                null,          // recurringJobId
                null,          // recurringJobIds
                null,          // awaitingOn
                parentId,          // parentId
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

    @Override
    public Optional<JobExecutionInfo> getJobExecutionById(UUID jobId) {
        try {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            String jobType = job.getLabels().stream()
                    .filter(label -> label.startsWith("jobtype:"))
                    .map(label -> label.substring(8))
                    .findFirst()
                    .orElse(null);

            // Check for exactly one child job - this indicates a batch job execution
            AmountRequest amountRequest = new AmountRequest("updatedAt:DESC", 1);
            List<Job> childJobs = storageProvider.getJobList(createSearchRequestForParentId(job.getId()), amountRequest);
            if (childJobs.size() == 1) {
                Job childJob = childJobs.getFirst();
                return Optional.of(mapToJobExecutionInfo(jobType, childJob));
            }
            return Optional.of(mapToJobExecutionInfo(jobType, job));
        } catch (Exception e) {
            log.error("Fehler beim Abrufen von Job {}", jobId, e);
            return Optional.empty();
        }
    }

    @Override
    public String getDashboardDeepLink(UUID jobId) {
        return dashboardUrl + "/dashboard/jobs/" + jobId;
    }


    private JobExecutionInfo mapToJobExecutionInfo(JobDefinition parentJobDefinition, org.jobrunr.jobs.Job parent, org.jobrunr.jobs.Job child) {
        JobExecutionInfo childInfo = mapToJobExecutionInfo(parentJobDefinition.type(), child);

        childInfo = new JobExecutionInfo(
                childInfo.getJobId(),
                parent.getJobName() + " (Batch)",
                parentJobDefinition.type(),
                childInfo.getStatus(),
                childInfo.getStartedAt(),
                childInfo.getFinishedAt().orElse(null),
                childInfo.getBatchProgress().orElse(null),
                childInfo.getParameters()
        );

        return childInfo;
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

    private String extractSimpleClassName(String fullyQualifiedClassName) {
        int lastDotIndex = fullyQualifiedClassName.lastIndexOf('.');
        return lastDotIndex >= 0 ? fullyQualifiedClassName.substring(lastDotIndex + 1) : fullyQualifiedClassName;
    }

    private JobStatus mapJobState(JobState jobState) {
        if (jobState instanceof EnqueuedState || jobState instanceof ScheduledState || jobState instanceof AwaitingState) {
            return JobStatus.ENQUEUED;
        } else if (jobState instanceof ProcessingState) {
            return JobStatus.PROCESSING;
        } else if (jobState instanceof SucceededState) {
            return JobStatus.SUCCEEDED;
        } else if (jobState instanceof FailedState) {
            return JobStatus.FAILED;
        } else {
            return JobStatus.ENQUEUED;
        }
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
