package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.adapter.ui.PaginationHelper;
import ch.css.jobrunr.control.adapter.ui.TemplateExtensions;
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
import java.util.UUID;

@ApplicationScoped
public class GetJobDetailsMessageUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsRecapUseCase.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // Type alias for pagination result
    public record MessagesPaginationResult(
            List<JobMessage> pageItems,
            PaginationHelper.PaginationMetadata pagination,
            List<TemplateExtensions.PageItem> pageRange
    ) {}

    private final StorageProvider storageProvider;

    @Inject
    public GetJobDetailsMessageUseCase(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public MessagesPaginationResult execute(UUID jobId, SearchMessageLevel search, int page, int size) {
        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            BatchJob batchJob = (BatchJob) jobById;
            List<JobMessage> allMessages = getMessages(batchJob, search);
            PaginationHelper.PaginationResult<JobMessage> paginationResult = PaginationHelper.paginate(allMessages, page, size);
            return new MessagesPaginationResult(paginationResult.pageItems(), paginationResult.metadata(), paginationResult.pageRange());
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    public List<JobMessage> execute(UUID jobId, SearchMessageLevel search) {
        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            BatchJob batchJob = (BatchJob) jobById;
            return getMessages(batchJob, search);
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }

    private List<JobMessage> getMessages(BatchJob batchJob, SearchMessageLevel search) {
        final List<JobMessage> messages = new ArrayList<>();
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
                    if(message.getLevel() == JobDashboardLogger.Level.INFO && (search == SearchMessageLevel.ALL || search == SearchMessageLevel.INFO_ONLY)) {
                        messages.add(new JobMessage(message.getLogInstant(), MessageLevel.INFO, message.getLogMessage(), formatInstant(message.getLogInstant())));
                    } else if(message.getLevel() == JobDashboardLogger.Level.WARN && (search == SearchMessageLevel.ALL || search == SearchMessageLevel.WARNING_ONLY || search == SearchMessageLevel.WARNINGS_AND_ERRORS)) {
                        messages.add(new JobMessage(message.getLogInstant(), MessageLevel.WARNING, message.getLogMessage(), formatInstant(message.getLogInstant())));
                    } else if(message.getLevel() == JobDashboardLogger.Level.ERROR && (search == SearchMessageLevel.ALL || search == SearchMessageLevel.ERROR_ONLY || search == SearchMessageLevel.WARNINGS_AND_ERRORS)) {
                        messages.add(new JobMessage(message.getLogInstant(), MessageLevel.ERROR, message.getLogMessage(), formatInstant(message.getLogInstant())));
                    }
                });
        return messages;
    }

    private String formatInstant(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
    }


    public record JobMessage(
            Instant createdAt,
            MessageLevel messageLevel,
            String message,
            String formattedCreatedAt
    ) {}

    public enum MessageLevel {
        INFO,
        WARNING,
        ERROR
    }

    public enum SearchMessageLevel {
        ALL,
        WARNINGS_AND_ERRORS,
        INFO_ONLY,
        WARNING_ONLY,
        ERROR_ONLY
    }
}
