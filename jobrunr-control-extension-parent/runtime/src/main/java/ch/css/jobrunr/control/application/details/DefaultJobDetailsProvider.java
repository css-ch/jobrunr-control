package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.adapter.ui.PaginationHelper;
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

import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class DefaultJobDetailsProvider implements JobMessageProvider, JobRecapProvider {

    private final JobExecutionPort jobExecutionPort;
    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

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
    public Map<String, Object> determineRecap(UUID jobId, String jobType) {
        Job job = storageProvider.getJobById(jobId);
        if(!job.isBatchJob()) {
            return Map.of();
        }
        BatchJob batchJob = (BatchJob) job;
        List<Job> childJobs = getChildJobs(batchJob);

        JobExecutionInfo jobExecutionInfo = jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> {
                    return new JobNotFoundException("Job execution with ID " + jobId + " not found");
                });
        String effectiveJobType = resolveJobType(jobExecutionInfo, jobType);
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(effectiveJobType);

        Map<String, AtomicLong> counters = new HashMap<>();
        for (JobRecapParameter recapParameter : jobDefinition.recapParameters()) {
            counters.put(recapParameter.name(), new AtomicLong(0));
        }
        childJobs.forEach(childJob -> updateCounters(counters, childJob.getResult()));

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

    @Override
    public PagedJobMessages searchJobMessages(UUID jobId, JobMessageSearch searchFilter, int pageNumber, int pageSize) {
        Job job = storageProvider.getJobById(jobId);
        if(!job.isBatchJob()) {
            return new PagedJobMessages(List.of(), 0, 0, 0);
        }
        BatchJob batchJob = (BatchJob) job;
        List<JobMessage> allMessages = getMessages(batchJob, searchFilter);
        PaginationHelper.PaginationResult<JobMessage> paginationResult = PaginationHelper.paginate(allMessages, pageNumber, pageSize);
        return new PagedJobMessages(paginationResult.pageItems(), paginationResult.metadata().totalElements(), paginationResult.metadata().page(), paginationResult.metadata().size());
    }


    private List<JobMessage> getMessages(BatchJob batchJob, JobMessageSearch search) {
        final List<JobMessage> messages = new ArrayList<>();
        getChildJobs(batchJob)
                .forEach(job -> job.getMetadata().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                        .map(Map.Entry::getValue)
                        .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                        .map(o -> (JobDashboardLogger.JobDashboardLogLines) o)
                        .flatMap(ll -> ll.getLogLines().stream())
                        .forEach(message -> {
                            String stackTrace = resolveStackTrace(job, message);
                            if (matchesSearch(message.getLevel(), stackTrace != null && !stackTrace.isBlank(), search)) {
                                messages.add(new JobMessage(
                                        message.getLogInstant(),
                                        toJobMessageLevel(message.getLevel(), stackTrace != null && !stackTrace.isBlank()),
                                        message.getLogMessage(),
                                        stackTrace
                                ));
                            }
                        }));
        return messages;
    }

    private String resolveStackTrace(Job childJob, JobDashboardLogger.JobDashboardLogLine message) {
        if (message.getLevel() != JobDashboardLogger.Level.ERROR) {
            return null;
        }
        return childJob.getLastJobStateOfType(FailedState.class)
                .map(FailedState::getStackTrace)
                .filter(stackTrace -> stackTrace != null && !stackTrace.isBlank())
                .orElse(null);
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

    private JobMessageLevel toJobMessageLevel(JobDashboardLogger.Level level, boolean hasStackTrace) {
        return switch (level) {
            case INFO -> JobMessageLevel.INFO;
            case WARN -> JobMessageLevel.WARNING;
            case ERROR -> hasStackTrace ? JobMessageLevel.EXCEPTION : JobMessageLevel.ERROR;
        };
    }

    @Override
    public JobMessageCounter determineJobMessageCounter(UUID jobId) {
        Job job = storageProvider.getJobById(jobId);
        if(!job.isBatchJob()) {
            return new JobMessageCounter(0,0,0,0);
        }
        BatchJob batchJob = (BatchJob) job;
        AtomicLong infoMessages = new AtomicLong(0);
        AtomicLong warningMessages = new AtomicLong(0);
        AtomicLong errorMessages = new AtomicLong(0);
        AtomicLong exceptionMessages = new AtomicLong(0);

        getChildJobs(batchJob).forEach(childJob -> childJob.getMetadata().entrySet().stream()
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
                            if (hasStackTrace(childJob, message)) {
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
}
