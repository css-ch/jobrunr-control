package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.*;
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

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class GetJobDetailsRecapUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsRecapUseCase.class);
    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.GERMAN);

    private final JobExecutionPort jobExecutionPort;
    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Inject
    public GetJobDetailsRecapUseCase(JobExecutionPort jobExecutionPort,
                                     StorageProvider storageProvider,
                                     JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                     JobDetailsProviderRegistry jobDetailsProviderRegistry) {
        this.jobExecutionPort = jobExecutionPort;
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
    }

    public Result execute(UUID jobId) {
        return execute(jobId, null);
    }

    public Result execute(UUID jobId, String jobType) {
        JobExecutionInfo jobExecutionInfo = jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> {
                    LOG.errorf("Job execution not found: %s", jobId);
                    return new JobNotFoundException("Job execution with ID " + jobId + " not found");
                });

        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            BatchJob batchJob = (BatchJob) jobById;
            String effectiveJobType = resolveJobType(jobExecutionInfo, jobType);
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(effectiveJobType);

            JobStatusAndTimestamp jobStatusAndTimestamp = evaluateJobStatusAndTimestamp(jobExecutionInfo);
            MessageCount messageCount = evaluateMessageCount(batchJob, jobId, effectiveJobType, jobDefinition);
            ChildJobCounters childJobCounters = evaluateChildJobCounters(batchJob);
            JobDurations jobDurations = evaluateJobDurations(jobExecutionInfo, childJobCounters.succeededChildJobCount);
            RecapCounters recapCounters = evaluateRecapCounters(batchJob, jobId, effectiveJobType, jobDefinition);

            return new Result(
                    jobStatusAndTimestamp,
                    messageCount,
                    jobDurations,
                    childJobCounters,
                    recapCounters
            );
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    private String resolveJobType(JobExecutionInfo jobExecutionInfo, String jobType) {
        if (jobType != null && !jobType.isBlank()) {
            return jobType;
        }
        return jobExecutionInfo.jobType();
    }

    private List<Job> getChildJobs(BatchJob batchJob) {
        return storageProvider.getJobList(JobSearchRequestBuilder.aJobSearchRequest()
                .withParentId(batchJob.getId())
                .build(), AmountRequest.fromString("limit=1000000"));
    }

    private JobStatusAndTimestamp evaluateJobStatusAndTimestamp(JobExecutionInfo jobExecutionInfo) {
        return new JobStatusAndTimestamp(
                jobExecutionInfo.getStatus(),
                jobExecutionInfo.startedAt(),
                jobExecutionInfo.getFinishedAt().orElse(null)
        );
    }

    private MessageCount evaluateMessageCount(BatchJob batchJob, UUID jobId, String jobType, JobDefinition jobDefinition) {
        Optional<JobMessageProvider> jobMessageProvider = resolveMessageProvider(jobDefinition);
        if (jobMessageProvider.isPresent()) {
            JobMessageCounter counter = jobMessageProvider.get().determineJobMessageCounter(jobId, jobType);
            return new MessageCount(counter.totalMessages(), counter.infoMessages(), counter.warningMessages(), counter.errorMessages());
        }

        return evaluateMessageCount(getChildJobs(batchJob));
    }

    private MessageCount evaluateMessageCount(List<Job> childJobs) {
        final AtomicLong infoMessages = new AtomicLong(0);
        final AtomicLong warningMessages = new AtomicLong(0);
        final AtomicLong errorMessages = new AtomicLong(0);
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
        long totalMessages = infoMessages.get() + warningMessages.get() + errorMessages.get();
        return new MessageCount(totalMessages, infoMessages.get(), warningMessages.get(), errorMessages.get());
    }

    private ChildJobCounters evaluateChildJobCounters(BatchJob batchJob) {
        BatchJob.BatchJobStats batchJobStats = getBatchJobStats(batchJob);
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
        Instant finishedAt = jobExecutionInfo.getFinishedAt().orElse(null);

        if (startedAt == null || succeededChildJobCount <= 0) {
            return new JobDurations("--", "Minuten", "--", "Minuten");
        }

        // Use current time if finishedAt is null
        Instant endTime = finishedAt != null ? finishedAt : Instant.now();

        // Calculate total duration in milliseconds
        long totalDurationMillis = endTime.toEpochMilli() - startedAt.toEpochMilli();

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
        DecimalFormat decimalFormat = new DecimalFormat("0.##", DECIMAL_FORMAT_SYMBOLS);
        return decimalFormat.format(value);
    }

    private RecapCounters evaluateRecapCounters(BatchJob batchJob, UUID jobId, String jobType, JobDefinition jobDefinition) {
        Optional<JobRecapProvider> jobRecapProvider = resolveRecapProvider(jobDefinition);
        Map<String, Object> counters = jobRecapProvider
                .map(provider -> provider.determineRecap(jobId, jobType))
                .orElseGet(() -> calculateRecapCounters(getChildJobs(batchJob), jobDefinition.recapParameters()));
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

    private Map<String, Object> calculateRecapCounters(List<Job> childJobs, List<JobRecapParameter> recapParameters) {
        Map<String, AtomicLong> counters = new HashMap<>();
        for (JobRecapParameter recapParameter : recapParameters) {
            counters.put(recapParameter.name(), new AtomicLong(0));
        }
        childJobs.forEach(job -> updateCounters(counters, job.getResult()));

        return counters.entrySet().stream()
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().get()), HashMap::putAll);
    }

    private void updateCounters(Map<String, AtomicLong> counters, Object recap) {
        if (recap == null) return;
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            try {
                Method method = recap.getClass().getMethod(entry.getKey());
                Object result = method.invoke(recap);
                if (result instanceof Number) {
                    entry.getValue().addAndGet(((Number) result).longValue());
                }
            } catch (Exception e) {
                // Handle exception
            }
        }
    }

    public record JobStatusAndTimestamp(
            JobStatus jobStatus,
            Instant startedAt,
            Instant finishedAt) {

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

    public record MessageCount(
            long totalMessages,
            long infoMessages,
            long warningMessages,
            long errorMessages) {
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
            Map<String, Object> recapCounters,
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
