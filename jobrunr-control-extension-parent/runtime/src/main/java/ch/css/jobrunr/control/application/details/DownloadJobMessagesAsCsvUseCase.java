package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.details.JobDetailsProviderRegistry;
import ch.css.jobrunr.control.domain.details.JobMessage;
import ch.css.jobrunr.control.domain.details.JobMessageLevelSearch;
import ch.css.jobrunr.control.domain.details.JobMessageProvider;
import ch.css.jobrunr.control.domain.details.JobMessageSortOrder;
import ch.css.jobrunr.control.domain.details.JobMessagesPaged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;

import java.util.List;
import java.util.UUID;

/**
 * Use case for downloading all job messages matching a filter as a CSV document.
 * <p>
 * Returns the CSV content as a {@link String}. The caller is responsible for
 * setting the appropriate HTTP response headers (Content-Type, Content-Disposition).
 * <p>
 * CSV column order: Zeitstempel, Level, jobId, Message, StackTrace
 */
@ApplicationScoped
public class DownloadJobMessagesAsCsvUseCase {

    private static final Logger LOG = Logger.getLogger(DownloadJobMessagesAsCsvUseCase.class);

    /** Maximum number of rows exported to avoid excessive memory consumption. */
    static final int MAX_EXPORT_SIZE = 100_000;

    private static final String CSV_HEADER = "Zeitstempel,Level,jobId,Message,StackTrace\r\n";

    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Inject
    public DownloadJobMessagesAsCsvUseCase(StorageProvider storageProvider,
                                           JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                           JobDetailsProviderRegistry jobDetailsProviderRegistry) {
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
    }

    /**
     * Fetches all messages for the given job that match the supplied filter and
     * returns them as an RFC 4180-compliant CSV string.
     *
     * @param jobId      the UUID of the batch job
     * @param jobType    the job type identifier used for provider lookup
     * @param levelSearch  level filter to apply
     * @param textSearch   free-text search term (may be {@code null} or blank)
     * @param sortOrder  sort order applied to the export
     * @return CSV content including header row
     */
    public String execute(UUID jobId,
                          String jobType,
                          JobMessageLevelSearch levelSearch,
                          String textSearch,
                          JobMessageSortOrder sortOrder) {
        LOG.infof("Generating CSV export for jobId %s, level=%s, textSearch=%s", jobId, levelSearch, textSearch);

        Job jobById = storageProvider.getJobById(jobId);
        if (!jobById.isBatchJob()) {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }

        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);
        JobMessageProvider provider = jobDetailsProviderRegistry.getMessageProvider(
                jobDefinition.jobDetailPage() != null ? jobDefinition.jobDetailPage().messageProviderKey() : null);

        JobMessagesPaged result = provider.searchJobMessages(jobId, levelSearch, textSearch, sortOrder, 0, MAX_EXPORT_SIZE);
        LOG.infof("Exporting %d of %d messages for jobId %s", result.messages().size(), result.totalMessages(), jobId);

        return buildCsv(result.messages());
    }

    private String buildCsv(List<JobMessage> messages) {
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        for (JobMessage msg : messages) {
            sb.append(escapeCsv(msg.createdAtFormatted())).append(',');
            sb.append(escapeCsv(msg.messageLevel().name())).append(',');
            sb.append(escapeCsv(msg.jobId() != null ? msg.jobId().toString() : "")).append(',');
            sb.append(escapeCsv(msg.message())).append(',');
            sb.append(escapeCsv(msg.stackTrace() != null ? msg.stackTrace() : ""));
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Escapes a single CSV field value according to RFC 4180.
     * Fields that contain commas, double-quotes, or line breaks are enclosed in
     * double-quotes; any embedded double-quote characters are doubled.
     */
    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuoting = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        if (needsQuoting) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

