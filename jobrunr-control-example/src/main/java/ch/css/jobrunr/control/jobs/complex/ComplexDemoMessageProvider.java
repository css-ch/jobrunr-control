package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.application.details.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ComplexDemoMessageProvider implements JobMessageProvider {

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
    public PagedJobMessages searchJobMessages(UUID jobId, JobMessageSearch searchFilter, int pageNumber, int pageSize) {
        List<JobMessage> allMessages = getMessages(jobId, searchFilter);
        int fromIndex = Math.min(pageNumber * pageSize, allMessages.size());
        int toIndex = Math.min(fromIndex + pageSize, allMessages.size());
        return new PagedJobMessages(allMessages.subList(fromIndex, toIndex), allMessages.size(), pageNumber, pageSize);
    }

    @Override
    public JobMessageCounter determineJobMessageCounter(UUID jobId) {
        AtomicLong infoMessages = new AtomicLong(0);
        AtomicLong warningMessages = new AtomicLong(0);
        AtomicLong errorMessages = new AtomicLong(0);
        AtomicLong exceptionMessages = new AtomicLong(0);

        getChildJobs(jobId).forEach(job -> job.getMetadata().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                .map(Map.Entry::getValue)
                .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                .map(JobDashboardLogger.JobDashboardLogLines.class::cast)
                .flatMap(lines -> lines.getLogLines().stream())
                .forEach(message -> {
                    switch (message.getLevel()) {
                        case INFO -> infoMessages.incrementAndGet();
                        case WARN -> warningMessages.incrementAndGet();
                        case ERROR -> {
                            if (hasStackTrace(job, message)) {
                                exceptionMessages.incrementAndGet();
                            } else {
                                errorMessages.incrementAndGet();
                            }
                        }
                    }
                }));

        return new JobMessageCounter(infoMessages.get(), warningMessages.get(), errorMessages.get(), exceptionMessages.get());
    }

    private boolean hasStackTrace(Job childJob, JobDashboardLogger.JobDashboardLogLine logLine) {
        if (logLine.getLevel() != JobDashboardLogger.Level.ERROR) {
            return false;
        }
        return childJob.getLastJobStateOfType(FailedState.class)
                .map(FailedState::getStackTrace)
                .filter(stackTrace -> stackTrace != null && !stackTrace.isBlank())
                .isPresent();
    }

    private List<JobMessage> getMessages(UUID jobId, JobMessageSearch searchFilter) {
        List<JobMessage> messages = new ArrayList<>();
        getChildJobs(jobId).forEach(job -> job.getMetadata().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                .map(Map.Entry::getValue)
                .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                .map(JobDashboardLogger.JobDashboardLogLines.class::cast)
                .flatMap(lines -> lines.getLogLines().stream())
                .forEach(message -> {
                    String stackTrace = resolveStackTrace(job, message);
                    if (matchesSearch(message.getLevel(), stackTrace != null && !stackTrace.isBlank(), searchFilter)) {
                        messages.add(new JobMessage(
                                message.getLogInstant(),
                                toLevel(message.getLevel(), stackTrace != null && !stackTrace.isBlank()),
                                message.getLogMessage(),
                                stackTrace
                        ));
                    }
                }));
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

    private boolean matchesSearch(JobDashboardLogger.Level level, boolean hasStackTrace, JobMessageSearch search) {
        return switch (level) {
            case INFO -> search == JobMessageSearch.ALL || search == JobMessageSearch.INFO_ONLY;
            case WARN -> search == JobMessageSearch.ALL || search == JobMessageSearch.WARNING_ONLY || search == JobMessageSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
            case ERROR -> search == JobMessageSearch.ALL
                    || search == JobMessageSearch.ERROR_ONLY && !hasStackTrace
                    || search == JobMessageSearch.EXCEPTION_ONLY && hasStackTrace
                    || search == JobMessageSearch.ERRORS_AND_EXCEPTIONS
                    || search == JobMessageSearch.WARNINGS_AND_ERRORS_AND_EXCEPTIONS;
        };
    }

    private JobMessageLevel toLevel(JobDashboardLogger.Level level, boolean hasStackTrace) {
        return switch (level) {
            case INFO -> JobMessageLevel.INFO;
            case WARN -> JobMessageLevel.WARNING;
            case ERROR -> hasStackTrace ? JobMessageLevel.EXCEPTION : JobMessageLevel.ERROR;
        };
    }
}

