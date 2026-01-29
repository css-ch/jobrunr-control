package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.infrastructure.jobrunr.ConfigurableJobSearchAdapter;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobParameterExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.states.*;
import org.jobrunr.storage.StorageProvider;

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

    private static final Logger log = Logger.getLogger(JobRunrExecutionAdapter.class);

    private final StorageProvider storageProvider;
    private final ConfigurableJobSearchAdapter configurableJobSearchAdapter;

    @Inject
    public JobRunrExecutionAdapter(
            StorageProvider storageProvider,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            ConfigurableJobSearchAdapter configurableJobSearchAdapter
    ) {
        this.storageProvider = storageProvider;
        this.configurableJobSearchAdapter = configurableJobSearchAdapter;
    }

    @Override
    public List<JobExecutionInfo> getJobExecutions() {
        List<StateName> relavantStates = List.of(
                StateName.ENQUEUED,
                StateName.AWAITING,
                StateName.PROCESSING,
                StateName.PROCESSED,
                StateName.SUCCEEDED,
                StateName.FAILED
        );
        return configurableJobSearchAdapter.getConfigurableJob(relavantStates)
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
            log.errorf(e, "Fehler beim Abrufen von Job %s", jobId);
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
        var metadata = job.getMetadata().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("jobRunr"))
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue
                ));

        return new JobExecutionInfo(
                job.getId(),
                jobName,
                jobType,
                status,
                startedAt,
                finishedAt,
                batchProgress,
                parameters,
                metadata
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
            case FailedBatchJobState s -> JobStatus.FAILED;
            case null -> JobStatus.ENQUEUED;
            default -> {
                log.warnf("Unexpected job state: %s. Defaulting to ENQUEUED.", jobState.getName());
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
