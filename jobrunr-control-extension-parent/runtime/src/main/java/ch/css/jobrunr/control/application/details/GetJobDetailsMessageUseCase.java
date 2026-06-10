package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.details.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;

import java.util.UUID;

@ApplicationScoped
public class GetJobDetailsMessageUseCase {

    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Inject
    public GetJobDetailsMessageUseCase(StorageProvider storageProvider,
                                       JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                                       JobDetailsProviderRegistry jobDetailsProviderRegistry) {
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.jobDetailsProviderRegistry = jobDetailsProviderRegistry;
    }

    public JobMessagesPaged execute(UUID jobId,
                                            String jobType,
                                            JobMessageLevelSearch search,
                                            String textSearch,
                                            JobMessageSortOrder sortOrder,
                                            int page,
                                            int size) {
        Job jobById = storageProvider.getJobById(jobId);
        if (jobById.isBatchJob()) {
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(jobType);
            JobMessageProvider provider = jobDetailsProviderRegistry.getMessageProvider(jobDefinition.jobDetailPage() != null ? jobDefinition.jobDetailPage().messageProviderKey() : null);
            return provider.searchJobMessages(jobId, search, textSearch, sortOrder, page, size);
        } else {
            throw new IllegalStateException("Job with ID " + jobId + " is not a batch job");
        }
    }
}
