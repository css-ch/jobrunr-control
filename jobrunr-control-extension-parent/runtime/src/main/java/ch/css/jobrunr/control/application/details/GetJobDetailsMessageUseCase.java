package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.adapter.ui.PaginationHelper;
import ch.css.jobrunr.control.adapter.ui.TemplateExtensions;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GetJobDetailsMessageUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsMessageUseCase.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // Type alias for pagination result
    public record MessagesPaginationResult(
            List<JobMessage> pageItems,
            PaginationHelper.PaginationMetadata pagination,
            List<TemplateExtensions.PageItem> pageRange
    ) {}

    private final StorageProvider storageProvider;
    private final JobExecutionPort jobExecutionPort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Inject
    public GetJobDetailsMessageUseCase(StorageProvider storageProvider,
                                       JobExecutionPort jobExecutionPort,
                                       JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                       JobDetailsProviderRegistry jobDetailsProviderRegistry) {
        this.storageProvider = storageProvider;
        this.jobExecutionPort = jobExecutionPort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
    }

    public MessagesPaginationResult execute(UUID jobId, JobMessageSearch search, int page, int size) {
        return execute(jobId, null, search, page, size);
    }

    public MessagesPaginationResult execute(UUID jobId, String jobType, JobMessageSearch search, int page, int size) {
        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            String effectiveJobType = resolveJobType(jobId, jobType);
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(effectiveJobType);
            Optional<JobMessageProvider> jobMessageProvider = resolveMessageProvider(jobDefinition);
            if (jobMessageProvider.isPresent()) {
                JobMessageProvider.PagedJobMessages pagedJobMessages = jobMessageProvider.get().searchJobMessages(jobId, effectiveJobType, search, page, size);
                return toMessagesPaginationResult(pagedJobMessages);
            }
            BatchJob batchJob = (BatchJob) jobById;
            List<JobMessage> allMessages = getMessages(batchJob, search);
            PaginationHelper.PaginationResult<JobMessage> paginationResult = PaginationHelper.paginate(allMessages, page, size);
            return new MessagesPaginationResult(paginationResult.pageItems(), paginationResult.metadata(), paginationResult.pageRange());
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    public List<JobMessage> execute(UUID jobId, JobMessageSearch search) {
        return execute(jobId, null, search);
    }

    public List<JobMessage> execute(UUID jobId, String jobType, JobMessageSearch search) {
        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            String effectiveJobType = resolveJobType(jobId, jobType);
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(effectiveJobType);
            Optional<JobMessageProvider> jobMessageProvider = resolveMessageProvider(jobDefinition);
            if (jobMessageProvider.isPresent()) {
                return jobMessageProvider.get().searchJobMessages(jobId, effectiveJobType, search, 0, Integer.MAX_VALUE).items();
            }
            BatchJob batchJob = (BatchJob) jobById;
            return getMessages(batchJob, search);
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    private MessagesPaginationResult toMessagesPaginationResult(JobMessageProvider.PagedJobMessages pagedJobMessages) {
        PaginationHelper.PaginationMetadata paginationMetadata = PaginationHelper.createPaginationMetadata(
                pagedJobMessages.page(),
                pagedJobMessages.size(),
                pagedJobMessages.totalItems()
        );
        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(paginationMetadata);
        return new MessagesPaginationResult(pagedJobMessages.items(), paginationMetadata, pageRange);
    }

    private Optional<JobMessageProvider> resolveMessageProvider(JobDefinition jobDefinition) {
        if (jobDefinition.jobDetailPage() == null || jobDefinition.jobDetailPage().messageProviderKey() == null || jobDefinition.jobDetailPage().messageProviderKey().isBlank()) {
            return Optional.empty();
        }

        Optional<JobMessageProvider> provider = jobDetailsProviderRegistry.findMessageProvider(jobDefinition.jobDetailPage().messageProviderKey());
        if (provider.isEmpty()) {
            LOG.warnf("Configured JobMessageProvider with key '%s' for jobType %s was not found. Falling back to default message lookup.",
                    jobDefinition.jobDetailPage().messageProviderKey(), jobDefinition.jobType());
        }
        return provider;
    }

    private String resolveJobType(UUID jobId, String jobType) {
        if (jobType != null && !jobType.isBlank()) {
            return jobType;
        }

        JobExecutionInfo jobExecutionInfo = jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job execution with ID " + jobId + " not found"));
        return jobExecutionInfo.jobType();
    }

    private List<JobMessage> getMessages(BatchJob batchJob, JobMessageSearch search) {
        final List<JobMessage> messages = new ArrayList<>();
        getChildJobs(batchJob).stream()
                .flatMap(job -> job.getMetadata().entrySet().stream())
                .filter(entry -> entry.getKey().startsWith("jobRunrDashboardLog-"))
                .map(Map.Entry::getValue)
                .filter(value -> value instanceof JobDashboardLogger.JobDashboardLogLines)
                .map(o -> (JobDashboardLogger.JobDashboardLogLines) o)
                .flatMap(ll -> ll.getLogLines().stream())
                .forEach(message -> {
                    if (matchesSearch(message.getLevel(), search)) {
                        messages.add(new JobMessage(
                                message.getLogInstant(),
                                toJobMessageLevel(message.getLevel()),
                                message.getLogMessage(),
                                formatInstant(message.getLogInstant())
                        ));
                    }
                });
        return messages;
    }

    private List<Job> getChildJobs(BatchJob batchJob) {
        return storageProvider.getJobList(JobSearchRequestBuilder.aJobSearchRequest()
                .withParentId(batchJob.getId())
                .build(), AmountRequest.fromString("limit=1000000"));
    }

    private boolean matchesSearch(JobDashboardLogger.Level level, JobMessageSearch search) {
        return switch (level) {
            case INFO -> search == JobMessageSearch.ALL || search == JobMessageSearch.INFO_ONLY;
            case WARN -> search == JobMessageSearch.ALL || search == JobMessageSearch.WARNING_ONLY || search == JobMessageSearch.WARNINGS_AND_ERRORS;
            case ERROR -> search == JobMessageSearch.ALL || search == JobMessageSearch.ERROR_ONLY || search == JobMessageSearch.WARNINGS_AND_ERRORS;
        };
    }

    private JobMessageLevel toJobMessageLevel(JobDashboardLogger.Level level) {
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
