package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.*;
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
import java.lang.reflect.Method;
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
    private final Clock clock = Clock.systemUTC();
    private final Duration snapshotTtl = SNAPSHOT_TTL;
    private final Map<UUID, SnapshotCacheEntry> snapshotCache = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<BatchDetailsSnapshot>> inFlightSnapshots = new ConcurrentHashMap<>();

    @Inject
    public DefaultJobDetailsProvider(JobExecutionPort jobExecutionPort, StorageProvider storageProvider, JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobExecutionPort = jobExecutionPort;
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
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
    public JobMessageCounter determineJobMessageCounter(UUID jobId) {
        Job job = storageProvider.getJobById(jobId);
        if (!job.isBatchJob()) {
            return new JobMessageCounter(0, 0, 0, 0);
        }
        BatchDetailsSnapshot snapshot = getOrBuildSnapshot(jobId);
        return snapshot.messageCounter();
    }


    @Override
    public PagedJobMessages searchJobMessages(UUID jobId,
                                              JobMessageLevelSearch levelSearch,
                                              String textSearch,
                                              JobMessageSortOrder sortOrder,
                                              int pageNumber,
                                              int pageSize) {
        Job job = storageProvider.getJobById(jobId);
        if (!job.isBatchJob()) {
            return new PagedJobMessages(List.of(), 0, 0, 0);
        }
        BatchDetailsSnapshot snapshot = getOrBuildSnapshot(jobId);
        return paginateMessages(snapshot.messages(), levelSearch, textSearch, sortOrder, pageNumber, pageSize);
    }

    private PagedJobMessages paginateMessages(List<CollectedMessage> source,
                                              JobMessageLevelSearch search,
                                              String textSearch,
                                              JobMessageSortOrder sortOrder,
                                              int pageNumber,
                                              int pageSize) {
        int sanitizedPage = Math.max(0, pageNumber);
        int sanitizedPageSize = pageSize <= 0 ? 25 : pageSize;
        int from = Math.max(0, sanitizedPage * sanitizedPageSize);

        Comparator<CollectedMessage> sortByDate = Comparator.comparing(CollectedMessage::createdAt);
        JobMessageSortOrder effectiveSortOrder = sortOrder == null ? JobMessageSortOrder.OLDEST_FIRST : sortOrder;

        List<CollectedMessage> filteredMessages = source.stream()
                .filter(message -> matchesSearch(message.level(), message.hasStackTrace(), search))
                .filter(message -> matchesTextSearch(message, textSearch))
                .sorted(effectiveSortOrder == JobMessageSortOrder.NEWEST_FIRST ? sortByDate.reversed() : sortByDate)
                .toList();

        long totalItems = filteredMessages.size();
        if (from >= filteredMessages.size()) {
            return new PagedJobMessages(List.of(), totalItems, sanitizedPage, sanitizedPageSize);
        }

        int toExclusive = Math.min(filteredMessages.size(), from + sanitizedPageSize);
        List<JobMessage> pageItems = filteredMessages.subList(from, toExclusive).stream()
                .map(CollectedMessage::toJobMessage)
                .toList();

        return new PagedJobMessages(pageItems, totalItems, sanitizedPage, sanitizedPageSize);
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

        AtomicLong infoMessages = new AtomicLong(0);
        AtomicLong warningMessages = new AtomicLong(0);
        AtomicLong errorMessages = new AtomicLong(0);
        AtomicLong exceptionMessages = new AtomicLong(0);
        List<CollectedMessage> messages = new ArrayList<>();

        for (Job childJob : getChildJobs(batchJob)) {
            updateCounters(recapCounters, childJob.getResult());
            Optional<FailedState> lastJobState = childJob.getLastJobStateOfType(FailedState.class);
            lastJobState.ifPresent(failedState -> {
                messages.add(new CollectedMessage(
                        failedState.getCreatedAt(),
                        JobDashboardLogger.Level.ERROR,
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
                                logLine.getLevel(),
                                logLine.getLogMessage(),
                                null
                        ));
                    });
        }

        return new BatchDetailsSnapshot(
                toLongMap(recapCounters),
                new JobMessageCounter(infoMessages.get(), warningMessages.get(), errorMessages.get(), exceptionMessages.get()),
                List.copyOf(messages)
        );
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

    private String resolveStackTrace(Job childJob) {
        return childJob.getLastJobStateOfType(FailedState.class)
                .map(FailedState::getStackTrace)
                .filter(stackTrace -> !stackTrace.isBlank())
                .orElse(null);
    }

    private boolean matchesSearch(JobDashboardLogger.Level level, boolean hasStackTrace, JobMessageLevelSearch search) {
        return switch (level) {
            case INFO -> search == JobMessageLevelSearch.ALL || search == JobMessageLevelSearch.INFO_ONLY;
            case WARN ->
                    search == JobMessageLevelSearch.ALL || search == JobMessageLevelSearch.WARNING_ONLY || search == JobMessageLevelSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
            case ERROR -> search == JobMessageLevelSearch.ALL
                    || search == JobMessageLevelSearch.ERROR_ONLY && !hasStackTrace
                    || search == JobMessageLevelSearch.EXCEPTION_ONLY && hasStackTrace
                    || search == JobMessageLevelSearch.ERRORS_AND_EXCEPTIONS
                    || search == JobMessageLevelSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
        };
    }

    private static JobMessageLevel toJobMessageLevel(JobDashboardLogger.Level level, boolean hasStackTrace) {
        return switch (level) {
            case INFO -> JobMessageLevel.INFO;
            case WARN -> JobMessageLevel.WARNING;
            case ERROR -> hasStackTrace ? JobMessageLevel.EXCEPTION : JobMessageLevel.ERROR;
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
                                    JobDashboardLogger.Level level,
                                    String message,
                                    String stackTrace) {

        boolean hasStackTrace() {
            return stackTrace != null && !stackTrace.isBlank();
        }

        JobMessage toJobMessage() {
            return new JobMessage(createdAt, toJobMessageLevel(level, hasStackTrace()), message, stackTrace);
        }
    }

    private record BatchDetailsSnapshot(Map<String, Long> recapCounters,
                                        JobMessageCounter messageCounter,
                                        List<CollectedMessage> messages) {

        static BatchDetailsSnapshot empty() {
            return new BatchDetailsSnapshot(Map.of(), new JobMessageCounter(0, 0, 0, 0), List.of());
        }
    }
}
