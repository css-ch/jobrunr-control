package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class GetJobDetailsRecapUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsRecapUseCase.class);
    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.GERMAN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final JobExecutionPort jobExecutionPort;
    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;
    private final DefaultJobDetailsProvider defaultJobDetailsProvider;

    @Inject
    public GetJobDetailsRecapUseCase(JobExecutionPort jobExecutionPort,
                                     StorageProvider storageProvider,
                                     JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                     JobDetailsProviderRegistry jobDetailsProviderRegistry, DefaultJobDetailsProvider defaultJobDetailsProvider) {
        this.jobExecutionPort = jobExecutionPort;
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
        this.defaultJobDetailsProvider = defaultJobDetailsProvider;
    }

    public Result execute(UUID jobId) {
        JobExecutionInfo jobExecutionInfo = jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> {
                    LOG.errorf("Job execution not found: %s", jobId);
                    return new JobNotFoundException("Job execution with ID " + jobId + " not found");
                });
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobExecutionInfo.getJobType());

        JobStatusAndTimestamp jobStatusAndTimestamp = evaluateJobStatusAndTimestamp(jobExecutionInfo);
        MessageCount messageCount = evaluateMessageCount(jobId, jobDefinition);
        ChildJobCounters childJobCounters = evaluateChildJobCounters(jobId);
        JobDurations jobDurations = evaluateJobDurations(jobExecutionInfo, childJobCounters.succeededChildJobCount);
        RecapCounters recapCounters = evaluateRecapCounters(jobId, jobDefinition);

        return new Result(
                jobStatusAndTimestamp,
                messageCount,
                jobDurations,
                childJobCounters,
                recapCounters
        );
    }

    private JobStatusAndTimestamp evaluateJobStatusAndTimestamp(JobExecutionInfo jobExecutionInfo) {
        return new JobStatusAndTimestamp(
                jobExecutionInfo.getStatus(),
                jobExecutionInfo.startedAt(),
                getFinishedAt(jobExecutionInfo)
        );
    }

    private Instant getFinishedAt(JobExecutionInfo jobExecutionInfo) {
        if(jobExecutionInfo.getStatus() == JobStatus.FAILED) {
            Job job = storageProvider.getJobById(jobExecutionInfo.getJobId());
            return job.getUpdatedAt();
        }
        return jobExecutionInfo.getFinishedAt().orElse(null);
    }

    private MessageCount evaluateMessageCount(UUID jobId, JobDefinition jobDefinition) {
        Optional<JobMessageProvider> jobMessageProvider = resolveMessageProvider(jobDefinition);
        JobMessageCounter counter;
        if (jobMessageProvider.isPresent()) {
            counter = jobMessageProvider.get().determineJobMessageCounter(jobId);
        } else {
            counter = defaultJobDetailsProvider.determineJobMessageCounter(jobId);
        }
        return new MessageCount(counter.totalMessages(), counter.infoMessages(), counter.warningMessages(), counter.errorMessages(), counter.exceptionMessages());
    }

    private ChildJobCounters evaluateChildJobCounters(UUID jobId) {
        Job job = storageProvider.getJobById(jobId);
        if (job.isBatchJob()) {
            BatchJob.BatchJobStats batchJobStats = getBatchJobStats((BatchJob) job);
            long totalChildJobs = batchJobStats.getTotalChildJobCount();
            long succeededChildJobCount = batchJobStats.getSucceededChildJobCount();
            long failedChildJobCount = batchJobStats.getFailedChildJobCount();
            long inProgressChildJobCount = Math.max(0, totalChildJobs - succeededChildJobCount - failedChildJobCount);
            long completedPercentage = totalChildJobs <= 0 ? 0 : (succeededChildJobCount * 100) / totalChildJobs;

            return new ChildJobCounters(
                    totalChildJobs,
                    succeededChildJobCount,
                    failedChildJobCount,
                    inProgressChildJobCount,
                    completedPercentage
            );
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    private BatchJob.BatchJobStats getBatchJobStats(BatchJob batchJob) {
        try {
            return batchJob.getBatchJobStats();
        } catch (IllegalStateException e) {
            LOG.warn("Batch job stats not found: " + batchJob.getId().toString());
        }
        return new BatchJob.BatchJobStats(0, 0, 0);
    }

    private JobDurations evaluateJobDurations(JobExecutionInfo jobExecutionInfo, long succeededChildJobCount) {
        Instant startedAt = jobExecutionInfo.startedAt();
        Instant finishedAt = getFinishedAt(jobExecutionInfo);
        if (startedAt == null || succeededChildJobCount <= 0) {
            return new JobDurations("--", "Minuten", "--", "Minuten");
        }
        if(finishedAt == null) {
            finishedAt = Instant.now();
        }
        // Calculate total duration in milliseconds
        long totalDurationMillis = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        // Calculate average time per child job
        long averageDurationMillis = totalDurationMillis / succeededChildJobCount;
        String[] totalDurationRep = determineDurationRepresentation(totalDurationMillis);
        String[] averageDurationRep = determineDurationRepresentation(averageDurationMillis);
        return new JobDurations(totalDurationRep[0], totalDurationRep[1], averageDurationRep[0], averageDurationRep[1]);
    }

    private String[] determineDurationRepresentation(long durationMillis) {
        if (durationMillis < 1000L) {
            return new String[]{formatDuration(durationMillis), "Millis"};
        } else if (durationMillis < 1000L * 60L) {
            return new String[]{formatDuration(durationMillis / 1000.0), "Sekunden"};
        } else if (durationMillis < 1000L * 60L * 60L) {
            return new String[]{formatDuration(durationMillis / (1000.0 * 60.0)), "Minuten"};
        } else if (durationMillis < 1000L * 60L * 60L * 24L) {
            return new String[]{formatDuration(durationMillis / (1000.0 * 60.0 * 60.0)), "Stunden"};
        } else {
            return new String[]{formatDuration(durationMillis / (1000.0 * 60.0 * 60.0 * 24.0)), "Tage"};
        }
    }

    private String formatDuration(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.#", DECIMAL_FORMAT_SYMBOLS);
        return decimalFormat.format(value);
    }

    private RecapCounters evaluateRecapCounters(UUID jobId, JobDefinition jobDefinition) {
        Optional<JobRecapProvider> jobRecapProvider = resolveRecapProvider(jobDefinition);
        Map<String, Long> counters = jobRecapProvider
                .map(provider -> provider.determineRecap(jobId))
                .orElseGet(() -> defaultJobDetailsProvider.determineRecap(jobId));
        return new RecapCounters(counters, evaluateRecapSettings(jobDefinition));
    }

    private Optional<JobMessageProvider> resolveMessageProvider(JobDefinition jobDefinition) {
        if (jobDefinition.jobDetailPage() == null || jobDefinition.jobDetailPage().messageProviderKey() == null || jobDefinition.jobDetailPage().messageProviderKey().isBlank()) {
            return Optional.empty();
        }

        Optional<JobMessageProvider> provider = jobDetailsProviderRegistry.findMessageProvider(jobDefinition.jobDetailPage().messageProviderKey());
        if (provider.isEmpty()) {
            LOG.warnf("Configured JobMessageProvider with key '%s' for jobType %s was not found. Falling back to default message counter lookup.",
                    jobDefinition.jobDetailPage().messageProviderKey(), jobDefinition.jobType());
        }
        return provider;
    }

    private Optional<JobRecapProvider> resolveRecapProvider(JobDefinition jobDefinition) {
        if (jobDefinition.jobDetailPage() == null || jobDefinition.jobDetailPage().recapProviderKey() == null || jobDefinition.jobDetailPage().recapProviderKey().isBlank()) {
            return Optional.empty();
        }

        Optional<JobRecapProvider> provider = jobDetailsProviderRegistry.findRecapProvider(jobDefinition.jobDetailPage().recapProviderKey());
        if (provider.isEmpty()) {
            LOG.warnf("Configured JobRecapProvider with key '%s' for jobType %s was not found. Falling back to default recap lookup.",
                    jobDefinition.jobDetailPage().recapProviderKey(), jobDefinition.jobType());
        }
        return provider;
    }

    private RecapSettings evaluateRecapSettings(JobDefinition jobDefinition) {
        return new RecapSettings(
                jobDefinition.recapParameters(),
                jobDefinition.jobDetailPage() != null && jobDefinition.jobDetailPage().showRecapParameterWithZeroValue()
        );
    }

    public record JobStatusAndTimestamp(
            JobStatus jobStatus,
            Instant startedAt,
            Instant finishedAt) {

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

    public record MessageCount(
            long totalMessages,
            long infoMessages,
            long warningMessages,
            long errorMessages,
            long exceptionMessages) {
    }

    public record JobDurations(
            String totalJobDuration,
            String totalJobDurationUnit,
            String averageChildDuration,
            String averageChildDurationUnit) {
    }

    public record ChildJobCounters(
            long totalChildJobs,
            long succeededChildJobCount,
            long failedChildJobCount,
            long inProgressChildJobCount,
            long completedPercentage) {
    }

    public record RecapSettings(
            List<JobRecapParameter> recapParameters,
            boolean showRecapParameterWithZeroValue) {
    }

    public record RecapCounters(
            Map<String, Long> recapCounters,
            RecapSettings recapSettings) {
    }

    public record Result(
            JobStatusAndTimestamp jobStatusAndTimestamp,
            MessageCount messageCount,
            JobDurations jobDurations,
            ChildJobCounters childJobCounters,
            RecapCounters recapCounters) {
    }
}
