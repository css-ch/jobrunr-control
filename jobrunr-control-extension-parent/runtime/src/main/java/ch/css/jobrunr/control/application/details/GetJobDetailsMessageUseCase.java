package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.adapter.ui.PaginationHelper;
import ch.css.jobrunr.control.adapter.ui.TemplateExtensions;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GetJobDetailsMessageUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsMessageUseCase.class);

    // Type alias for pagination result
    public record MessagesPaginationResult(
            List<JobMessage> pageItems,
            PaginationHelper.PaginationMetadata pagination,
            List<TemplateExtensions.PageItem> pageRange
    ) {
    }

    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;
    private final DefaultJobDetailsProvider defaultJobDetailsProvider;

    @Inject
    public GetJobDetailsMessageUseCase(StorageProvider storageProvider,
                                       JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                       JobDetailsProviderRegistry jobDetailsProviderRegistry, DefaultJobDetailsProvider defaultJobDetailsProvider) {
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
        this.defaultJobDetailsProvider = defaultJobDetailsProvider;
    }

    public MessagesPaginationResult execute(UUID jobId,
                                            String jobType,
                                            JobMessageLevelSearch search,
                                            String textSearch,
                                            JobMessageSortOrder sortOrder,
                                            int page,
                                            int size) {
        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);
            Optional<JobMessageProvider> jobMessageProvider = resolveMessageProvider(jobDefinition);
            JobMessageProvider.PagedJobMessages pagedJobMessages;
            if (jobMessageProvider.isPresent()) {
                pagedJobMessages = jobMessageProvider.get().searchJobMessages(jobId, search, textSearch, sortOrder, page, size);
            } else {
                pagedJobMessages = defaultJobDetailsProvider.searchJobMessages(jobId, search, textSearch, sortOrder, page, size);
            }
            return toMessagesPaginationResult(pagedJobMessages);
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
            LOG.warnf("Configured JobMessageProvider with key '%s' for jobType %s was not found. Falling back to default message lookup.", jobDefinition.jobDetailPage().messageProviderKey(), jobDefinition.jobType());
        }
        return provider;
    }
}
