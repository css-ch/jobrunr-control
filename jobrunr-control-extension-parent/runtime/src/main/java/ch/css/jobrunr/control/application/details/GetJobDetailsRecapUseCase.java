package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.JobStatus;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class GetJobDetailsRecapUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsRecapUseCase.class);

    private final JobExecutionPort jobExecutionPort;
    private final StorageProvider storageProvider;

    @Inject
    public GetJobDetailsRecapUseCase(JobExecutionPort jobExecutionPort, StorageProvider storageProvider) {
        this.jobExecutionPort = jobExecutionPort;
        this.storageProvider = storageProvider;
    }

    public Result execute(UUID jobId) {
        JobExecutionInfo jobExecutionInfo = jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> {
                    LOG.errorf("Job execution not found: %s", jobId);
                    return new JobNotFoundException("Job execution with ID " + jobId + " not found");
                });

        Job jobById = storageProvider.getJobById(jobId);
        BatchJob batchJob;
        if (jobById.isBatchJob()) {
            batchJob = (BatchJob) jobById;
            BatchJob.BatchJobStats batchJobStats = batchJob.getBatchJobStats();
            MessageCount counts = getMessagesCount(batchJob);
            return new Result(
                    jobExecutionInfo.getStatus(),
                    jobExecutionInfo.startedAt(),
                    jobExecutionInfo.getFinishedAt().orElse(null),
                    batchJobStats.getTotalChildJobCount(),
                    batchJobStats.getSucceededChildJobCount(),
                    batchJobStats.getFailedChildJobCount(),
                    batchJobStats.getTotalChildJobCount() - batchJobStats.getSucceededChildJobCount() - batchJobStats.getFailedChildJobCount(),
                    counts.infoMessages(),
                    counts.warningMessages(),
                    counts.errorMessages(),
                    getAverageTimePerChildMillis(jobExecutionInfo.startedAt(), jobExecutionInfo.getFinishedAt().orElse(null), batchJobStats.getSucceededChildJobCount()),
                    Map.of("Counter 1", 13, "Counter 2", 25, "Counter 3", 37) // TODO: recapCounters
            );
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    private MessageCount getMessagesCount(BatchJob batchJob) {
        final AtomicLong infoMessages = new AtomicLong(0);
        final AtomicLong warningMessages = new AtomicLong(0);
        final AtomicLong errorMessages = new AtomicLong(0);
        final List<Job> childJobs = storageProvider.getJobList(JobSearchRequestBuilder.aJobSearchRequest()
                .withParentId(batchJob.getId())
                .build(), AmountRequest.fromString("limit=1000000"));
        childJobs.stream()
                .flatMap(job -> job.getMetadata().entrySet().stream())
                .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                .map(Map.Entry::getValue)
                .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                .map(o -> (JobDashboardLogger.JobDashboardLogLines) o)
                .flatMap(ll -> ll.getLogLines().stream())
                .forEach(message -> {
                    switch (message.getLevel()) {
                        case JobDashboardLogger.Level.INFO -> infoMessages.incrementAndGet();
                        case JobDashboardLogger.Level.WARN -> warningMessages.incrementAndGet();
                        case JobDashboardLogger.Level.ERROR -> errorMessages.incrementAndGet();
                    }
                });
        return new MessageCount(infoMessages.get(), warningMessages.get(), errorMessages.get());
    }

    public record MessageCount(long infoMessages,
                               long warningMessages,
                               long errorMessages) {
    }

    public long getAverageTimePerChildMillis(Instant startedAt, Instant finishedAt, long succeededChildJobCount) {
        if (startedAt == null || succeededChildJobCount <= 0) {
            return 0L;
        }

        // Use current time if finishedAt is null
        Instant endTime = finishedAt != null ? finishedAt : Instant.now();

        // Calculate total duration in milliseconds
        long totalDurationMillis = endTime.toEpochMilli() - startedAt.toEpochMilli();

        // Calculate average time per child job
        return totalDurationMillis / succeededChildJobCount;
    }

    public record Result(
            JobStatus jobStatus,
            Instant startedAt,
            Instant finishedAt,
            long totalChildJobs,
            long succeededChildJobCount,
            long failedChildJobCount,
            long inProgressChildJobCount,
            long infoMessagesCount,
            long warningMessagesCount,
            long errorMessagesCount,
            long averageTimePerChildJob,
            Map<String, Object> recapCounters) {

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        public String getStartedAtFormatted() {
            if (startedAt == null) {
                return "--";
            }
            return DATE_TIME_FORMATTER.format(startedAt.atZone(ZoneId.systemDefault()));
        }

        public String getFinishedAtFormatted() {
            if (finishedAt == null) {
                return "--";
            }
            return DATE_TIME_FORMATTER.format(finishedAt.atZone(ZoneId.systemDefault()));
        }
    }
}
