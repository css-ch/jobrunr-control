package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.details.JobDetailsProviderRegistry;
import ch.css.jobrunr.control.domain.details.JobMessageLevelCounters;
import ch.css.jobrunr.control.domain.details.JobMessageProvider;
import ch.css.jobrunr.control.domain.details.JobRecapProvider;
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
    private static final String UNGROUPED_SECTION_KEY = "";

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
        RecapView recapView = evaluateRecapView(jobId, jobDefinition);

        return new Result(
                jobStatusAndTimestamp,
                messageCount,
                jobDurations,
                childJobCounters,
                recapView
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
        JobMessageProvider jobMessageProvider = jobDetailsProviderRegistry.getMessageProvider(jobDefinition.jobDetailPage() != null ? jobDefinition.jobDetailPage().messageProviderKey() : null);
        JobMessageLevelCounters counter = jobMessageProvider.determineJobMessageCounter(jobId);
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

    private RecapView evaluateRecapView(UUID jobId, JobDefinition jobDefinition) {
        JobRecapProvider jobRecapProvider = jobDetailsProviderRegistry.getRecapProvider(jobDefinition.jobDetailPage() != null ? jobDefinition.jobDetailPage().recapProviderKey() : null);
        Map<String, Long> counters = jobRecapProvider.determineRecap(jobId);
        return new RecapView(buildRecapSections(counters, jobDefinition));
    }

    private List<RecapSection> buildRecapSections(Map<String, Long> counters, JobDefinition jobDefinition) {
        List<JobRecapParameter> recapParameters = jobDefinition.recapParameters();
        boolean showZeroValues = jobDefinition.jobDetailPage() != null && jobDefinition.jobDetailPage().showRecapParameterWithZeroValue();
        Map<String, RecapSectionAccumulator> groupedSectionsByName = new LinkedHashMap<>();
        List<RecapSectionAccumulator> orderedSections = new ArrayList<>();

        for (JobRecapParameter parameter : recapParameters) {
            Long counter = counters.get(parameter.name());
            boolean visible = showZeroValues || (counter != null && counter > 0);
            if (!visible) {
                continue;
            }

            boolean hasSection = parameter.section() != null && !parameter.section().isBlank();
            RecapSectionAccumulator sectionAccumulator;
            if (hasSection) {
                sectionAccumulator = groupedSectionsByName.computeIfAbsent(parameter.section(), key -> {
                    RecapSectionAccumulator created = new RecapSectionAccumulator(key, true);
                    orderedSections.add(created);
                    return created;
                });
            } else {
                sectionAccumulator = new RecapSectionAccumulator(UNGROUPED_SECTION_KEY, false);
                orderedSections.add(sectionAccumulator);
            }
            sectionAccumulator.entries().add(new RecapEntry(parameter, counter));
        }

        return orderedSections.stream()
                .map(accumulator -> {
                    long total = accumulator.entries().stream()
                    .map(RecapEntry::counter)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
                    return new RecapSection(
                            accumulator.sectionName(),
                            accumulator.hasSection(),
                            List.copyOf(accumulator.entries()),
                            total
                    );
                })
                .toList();
    }

    private record RecapSectionAccumulator(
            String sectionName,
            boolean hasSection,
            List<RecapEntry> entries) {

        private RecapSectionAccumulator(String sectionName, boolean hasSection) {
            this(sectionName, hasSection, new ArrayList<>());
        }
    }

    public record JobStatusAndTimestamp(
            JobStatus jobStatus,
            Instant startedAt,
            Instant finishedAt) {

        @SuppressWarnings("unused")
        public String getStartedAtFormatted() {
            if (startedAt == null) {
                return "--";
            }
            return DATE_TIME_FORMATTER.format(startedAt.atZone(ZoneId.systemDefault()));
        }

        @SuppressWarnings("unused")
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

    public record RecapView(
            List<RecapSection> recapSections) {
    }

    public record RecapSection(
            String sectionName,
            boolean hasSection,
            List<RecapEntry> recapEntries,
            long sectionTotal) {
    }

    public record RecapEntry(
            JobRecapParameter recapParameter,
            Long counter) {
    }

    public record Result(
            JobStatusAndTimestamp jobStatusAndTimestamp,
            MessageCount messageCount,
            JobDurations jobDurations,
            ChildJobCounters childJobCounters,
            RecapView recapView) {
    }
}
