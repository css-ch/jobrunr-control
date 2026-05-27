package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.application.details.JobMessage;
import ch.css.jobrunr.control.application.details.JobMessageCounter;
import ch.css.jobrunr.control.application.details.JobMessageLevel;
import ch.css.jobrunr.control.application.details.JobMessageProvider;
import ch.css.jobrunr.control.application.details.JobMessageSearch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ComplexDemoMessageProvider implements JobMessageProvider {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final StorageProvider storageProvider;

    @Inject
    public ComplexDemoMessageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public String providerKey() {
        return "complex-demo-message-provider";
    }

    @Override
    public PagedJobMessages searchJobMessages(UUID jobId, String jobType, JobMessageSearch searchFilter, int pageNumber, int pageSize) {
        List<JobMessage> allMessages = getMessages(jobId, searchFilter);
        int fromIndex = Math.min(pageNumber * pageSize, allMessages.size());
        int toIndex = Math.min(fromIndex + pageSize, allMessages.size());
        return new PagedJobMessages(allMessages.subList(fromIndex, toIndex), allMessages.size(), pageNumber, pageSize);
    }

    @Override
    public JobMessageCounter determineJobMessageCounter(UUID jobId, String jobType) {
        AtomicLong infoMessages = new AtomicLong(0);
        AtomicLong warningMessages = new AtomicLong(0);
        AtomicLong errorMessages = new AtomicLong(0);

        getChildJobs(jobId).stream()
                .flatMap(job -> job.getMetadata().entrySet().stream())
                .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                .map(Map.Entry::getValue)
                .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                .map(JobDashboardLogger.JobDashboardLogLines.class::cast)
                .flatMap(lines -> lines.getLogLines().stream())
                .forEach(message -> {
                    switch (message.getLevel()) {
                        case INFO -> infoMessages.incrementAndGet();
                        case WARN -> warningMessages.incrementAndGet();
                        case ERROR -> errorMessages.incrementAndGet();
                    }
                });

        long totalMessages = infoMessages.get() + warningMessages.get() + errorMessages.get();
        return new JobMessageCounter(totalMessages, infoMessages.get(), warningMessages.get(), errorMessages.get());
    }

    private List<JobMessage> getMessages(UUID jobId, JobMessageSearch searchFilter) {
        List<JobMessage> messages = new ArrayList<>();
        getChildJobs(jobId).forEach(job -> job.getMetadata().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                .map(Map.Entry::getValue)
                .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                .map(JobDashboardLogger.JobDashboardLogLines.class::cast)
                .flatMap(lines -> lines.getLogLines().stream())
                .filter(logLine -> matchesSearch(logLine.getLevel(), searchFilter))
                .forEach(logLine -> messages.add(new JobMessage(
                        logLine.getLogInstant(),
                        toLevel(logLine.getLevel()),
                        logLine.getLogMessage(),
                        formatInstant(logLine.getLogInstant()),
                        resolveStackTrace(job, logLine)
                ))));
        return messages;
    }

    private String resolveStackTrace(Job childJob, JobDashboardLogger.JobDashboardLogLine logLine) {
        if (logLine.getLevel() != JobDashboardLogger.Level.ERROR) {
            return null;
        }
        return childJob.getLastJobStateOfType(FailedState.class)
                .map(FailedState::getStackTrace)
                .filter(stackTrace -> stackTrace != null && !stackTrace.isBlank())
                .orElse(null);
    }

    private List<Job> getChildJobs(UUID jobId) {
        return storageProvider.getJobList(JobSearchRequestBuilder.aJobSearchRequest()
                .withParentId(jobId)
                .build(), AmountRequest.fromString("limit=1000000"));
    }

    private boolean matchesSearch(JobDashboardLogger.Level level, JobMessageSearch searchFilter) {
        return switch (level) {
            case INFO -> searchFilter == JobMessageSearch.ALL || searchFilter == JobMessageSearch.INFO_ONLY;
            case WARN -> searchFilter == JobMessageSearch.ALL || searchFilter == JobMessageSearch.WARNING_ONLY || searchFilter == JobMessageSearch.WARNINGS_AND_ERRORS;
            case ERROR -> searchFilter == JobMessageSearch.ALL || searchFilter == JobMessageSearch.ERROR_ONLY || searchFilter == JobMessageSearch.WARNINGS_AND_ERRORS;
        };
    }

    private JobMessageLevel toLevel(JobDashboardLogger.Level level) {
        return switch (level) {
            case INFO -> JobMessageLevel.INFO;
            case WARN -> JobMessageLevel.WARNING;
            case ERROR -> JobMessageLevel.ERROR;
        };
    }

    private String formatInstant(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
    }
}

