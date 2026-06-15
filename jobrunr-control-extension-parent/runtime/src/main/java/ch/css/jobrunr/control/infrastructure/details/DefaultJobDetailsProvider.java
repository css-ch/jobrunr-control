package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.details.*;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class DefaultJobDetailsProvider implements JobMessageProvider, JobRecapProvider {
    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(2);

    private final JobExecutionPort jobExecutionPort;
    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final RecapValueExtractorRegistry recapValueExtractorRegistry;
    private final Clock clock = Clock.systemUTC();
    private final Duration snapshotTtl = SNAPSHOT_TTL;
    private final Map<UUID, SnapshotCacheEntry> snapshotCache = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<BatchDetailsSnapshot>> inFlightSnapshots = new ConcurrentHashMap<>();

    @Inject
    public DefaultJobDetailsProvider(JobExecutionPort jobExecutionPort,
                                     StorageProvider storageProvider,
                                     JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                     RecapValueExtractorRegistry recapValueExtractorRegistry) {
        this.jobExecutionPort = jobExecutionPort;
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.recapValueExtractorRegistry = recapValueExtractorRegistry;
    }

    @Override
    public String providerKey() {
        return "";
    }

    @Override
    public Map<String, Long> determineRecap(UUID jobId) {
        Job job = storageProvider.getJobById(jobId);
        if (!job.isBatchJob()) {
            return Map.of();
        }
        BatchDetailsSnapshot snapshot = getOrBuildSnapshot(jobId);
        return snapshot.recapCounters().entrySet().stream()
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
    }

    @Override
    public JobMessageLevelCounters determineJobMessageCounter(UUID jobId) {
        Job job = storageProvider.getJobById(jobId);
        if (!job.isBatchJob()) {
            return new JobMessageLevelCounters(0, 0, 0, 0);
        }
        BatchDetailsSnapshot snapshot = getOrBuildSnapshot(jobId);
        return snapshot.messageCounter();
    }


    @Override
    public JobMessagesPaged searchJobMessages(UUID jobId,
                                              JobMessageLevelSearch levelSearch,
                                              String textSearch,
                                              JobMessageSortOrder sortOrder,
                                              int pageNumber,
                                              int pageSize) {
        Job job = storageProvider.getJobById(jobId);
        if (!job.isBatchJob()) {
            return new JobMessagesPaged(List.of(), 0, 0, 0);
        }
        BatchDetailsSnapshot snapshot = getOrBuildSnapshot(jobId);
        return paginateMessages(snapshot.messages(), levelSearch, textSearch, sortOrder, pageNumber, pageSize);
    }

    private JobMessagesPaged paginateMessages(List<CollectedMessage> source,
                                              JobMessageLevelSearch search,
                                              String textSearch,
                                              JobMessageSortOrder sortOrder,
                                              int pageNumber,
                                              int pageSize) {
        int sanitizedPage = Math.max(0, pageNumber);
        int sanitizedPageSize = pageSize <= 0 ? 10 : pageSize;
        int from = Math.max(0, sanitizedPage * sanitizedPageSize);

        Comparator<CollectedMessage> sortByDate = Comparator.comparing(CollectedMessage::createdAt);
        JobMessageSortOrder effectiveSortOrder = sortOrder == null ? JobMessageSortOrder.OLDEST_FIRST : sortOrder;

        List<CollectedMessage> filteredMessages = source.stream()
                .filter(message -> matchesSearch(message.level(), search))
                .filter(message -> matchesTextSearch(message, textSearch))
                .sorted(effectiveSortOrder == JobMessageSortOrder.NEWEST_FIRST ? sortByDate.reversed() : sortByDate)
                .toList();

        long totalItems = filteredMessages.size();
        if (from >= filteredMessages.size()) {
            return new JobMessagesPaged(List.of(), totalItems, sanitizedPage, sanitizedPageSize);
        }

        int toExclusive = Math.min(filteredMessages.size(), from + sanitizedPageSize);
        List<JobMessage> pageItems = filteredMessages.subList(from, toExclusive).stream()
                .map(CollectedMessage::toJobMessage)
                .toList();

        return new JobMessagesPaged(pageItems, totalItems, sanitizedPage, sanitizedPageSize);
    }

    private boolean matchesTextSearch(CollectedMessage message, String textSearch) {
        if (textSearch == null || textSearch.isBlank()) {
            return true;
        }
        String normalizedSearch = textSearch.toLowerCase(Locale.ROOT);
        boolean inMessage = message.message() != null && message.message().toLowerCase(Locale.ROOT).contains(normalizedSearch);
        boolean inStackTrace = message.stackTrace() != null && message.stackTrace().toLowerCase(Locale.ROOT).contains(normalizedSearch);
        return inMessage || inStackTrace;
    }

    private BatchDetailsSnapshot getOrBuildSnapshot(UUID jobId) {
        long now = clock.millis();
        SnapshotCacheEntry cachedSnapshot = snapshotCache.get(jobId);
        if (cachedSnapshot != null && now - cachedSnapshot.createdAtMillis() < snapshotTtl.toMillis()) {
            return cachedSnapshot.snapshot();
        }

        CompletableFuture<BatchDetailsSnapshot> pendingFuture = new CompletableFuture<>();
        CompletableFuture<BatchDetailsSnapshot> existingFuture = inFlightSnapshots.putIfAbsent(jobId, pendingFuture);
        if (existingFuture != null) {
            return existingFuture.join();
        }

        try {
            BatchDetailsSnapshot snapshot = buildSnapshot(jobId);
            // TTL starts after the expensive snapshot build has completed.
            snapshotCache.put(jobId, new SnapshotCacheEntry(snapshot, clock.millis()));
            pendingFuture.complete(snapshot);
            return snapshot;
        } catch (RuntimeException e) {
            pendingFuture.completeExceptionally(e);
            throw e;
        } finally {
            inFlightSnapshots.remove(jobId, pendingFuture);
        }
    }

    private BatchDetailsSnapshot buildSnapshot(UUID jobId) {
        Job batch = storageProvider.getJobById(jobId);
        if (!batch.isBatchJob()) {
            return BatchDetailsSnapshot.empty();
        }
        BatchJob batchJob = (BatchJob) batch;

        JobExecutionInfo jobExecutionInfo = jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job execution with ID " + jobId + " not found"));
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobExecutionInfo.getJobType());
        Map<String, AtomicLong> recapCounters = initializeRecapCounters(jobDefinition);
        RecapValueExtractor recapValueExtractor = resolveRecapValueExtractor(jobDefinition);

        AtomicLong infoMessages = new AtomicLong(0);
        AtomicLong warningMessages = new AtomicLong(0);
        AtomicLong errorMessages = new AtomicLong(0);
        AtomicLong exceptionMessages = new AtomicLong(0);
        List<CollectedMessage> messages = new ArrayList<>();

        for (Job childJob : getChildJobs(batchJob)) {
            updateCounters(recapCounters, recapValueExtractor, childJob.getResult());
            Optional<FailedState> lastJobState = childJob.getLastJobStateOfType(FailedState.class);
            lastJobState.ifPresent(failedState -> {
                messages.add(new CollectedMessage(
                        failedState.getCreatedAt(),
                        childJob.getId(),
                        JobMessageLevel.EXCEPTION,
                        "[" + childJob.getJobName() + "] " + failedState.getExceptionMessage(),
                        failedState.getStackTrace()
                ));
                exceptionMessages.incrementAndGet();
            });

            childJob.getMetadata().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                    .map(Map.Entry::getValue)
                    .filter(JobDashboardLogger.JobDashboardLogLines.class::isInstance)
                    .map(JobDashboardLogger.JobDashboardLogLines.class::cast)
                    .flatMap(lines -> lines.getLogLines().stream())
                    .forEach(logLine -> {
                        switch (logLine.getLevel()) {
                            case INFO -> infoMessages.incrementAndGet();
                            case WARN -> warningMessages.incrementAndGet();
                            case ERROR -> errorMessages.incrementAndGet();
                        }
                        messages.add(new CollectedMessage(
                                logLine.getLogInstant(),
                                childJob.getId(),
                                toJobMessageLevel(logLine.getLevel()),
                                logLine.getLogMessage(),
                                null
                        ));
                    });
        }

        return new BatchDetailsSnapshot(
                toLongMap(recapCounters),
                new JobMessageLevelCounters(infoMessages.get(), warningMessages.get(), errorMessages.get(), exceptionMessages.get()),
                List.copyOf(messages)
        );
    }

    private RecapValueExtractor resolveRecapValueExtractor(JobDefinition jobDefinition) {
        if (jobDefinition.jobDetailPage() == null || jobDefinition.jobDetailPage().recapParameterClass() == null
                || jobDefinition.jobDetailPage().recapParameterClass().isBlank()) {
            return null;
        }
        return recapValueExtractorRegistry.findByRecapClassName(jobDefinition.jobDetailPage().recapParameterClass())
                .orElse(null);
    }

    private void updateCounters(Map<String, AtomicLong> counters, RecapValueExtractor recapValueExtractor, Object recap) {
        if (recap == null || recapValueExtractor == null) {
            return;
        }
        Map<String, Long> extractedValues = recapValueExtractor.extract(recap);
        for (Map.Entry<String, Long> extractedEntry : extractedValues.entrySet()) {
            AtomicLong counter = counters.get(extractedEntry.getKey());
            if (counter != null && extractedEntry.getValue() != null) {
                counter.addAndGet(extractedEntry.getValue());
            }
        }
    }

    private Map<String, AtomicLong> initializeRecapCounters(JobDefinition jobDefinition) {
        Map<String, AtomicLong> counters = new HashMap<>();
        for (JobRecapParameter recapParameter : jobDefinition.recapParameters()) {
            counters.put(recapParameter.name(), new AtomicLong(0));
        }
        return counters;
    }

    private Map<String, Long> toLongMap(Map<String, AtomicLong> counters) {
        Map<String, Long> result = new HashMap<>();
        counters.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    private boolean matchesSearch(JobMessageLevel level, JobMessageLevelSearch search) {
        return switch (level) {
            case INFO -> search == JobMessageLevelSearch.ALL
                    || search == JobMessageLevelSearch.INFO_ONLY;
            case WARNING -> search == JobMessageLevelSearch.ALL
                    || search == JobMessageLevelSearch.WARNING_ONLY
                    || search == JobMessageLevelSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
            case ERROR -> search == JobMessageLevelSearch.ALL
                    || search == JobMessageLevelSearch.ERROR_ONLY
                    || search == JobMessageLevelSearch.ERRORS_AND_EXCEPTIONS
                    || search == JobMessageLevelSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
            case EXCEPTION -> search == JobMessageLevelSearch.ALL
                    || search == JobMessageLevelSearch.EXCEPTION_ONLY
                    || search == JobMessageLevelSearch.ERRORS_AND_EXCEPTIONS
                    || search == JobMessageLevelSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
        };
    }

    private static JobMessageLevel toJobMessageLevel(JobDashboardLogger.Level level) {
        return switch (level) {
            case INFO -> JobMessageLevel.INFO;
            case WARN -> JobMessageLevel.WARNING;
            case ERROR -> JobMessageLevel.ERROR;
        };
    }

    private List<Job> getChildJobs(BatchJob batchJob) {
        return storageProvider.getJobList(JobSearchRequestBuilder.aJobSearchRequest()
                .withParentId(batchJob.getId())
                .build(), AmountRequest.fromString("limit=1000000"));
    }

    private record SnapshotCacheEntry(BatchDetailsSnapshot snapshot, long createdAtMillis) {
    }

    private record CollectedMessage(Instant createdAt,
                                    UUID jobId,
                                    JobMessageLevel level,
                                    String message,
                                    String stackTrace) {

        JobMessage toJobMessage() {
            return new JobMessage(createdAt, jobId, level, message, stackTrace);
        }
    }

    private record BatchDetailsSnapshot(Map<String, Long> recapCounters,
                                        JobMessageLevelCounters messageCounter,
                                        List<CollectedMessage> messages) {

        static BatchDetailsSnapshot empty() {
            return new BatchDetailsSnapshot(Map.of(), new JobMessageLevelCounters(0, 0, 0, 0), List.of());
        }
    }
}
