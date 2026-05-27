package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDetailPage;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSettings;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetJobDetailsMessageUseCase")
class GetJobDetailsMessageUseCaseTest {

    @Mock
    private StorageProvider storageProvider;


    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Mock
    private JobMessageProvider jobMessageProvider;

    @Mock
    private BatchJob batchJob;

    @InjectMocks
    private GetJobDetailsMessageUseCase useCase;

    @Test
    @DisplayName("should use configured custom message provider when present")
    void execute_CustomProviderConfigured_UsesProvider() {
        UUID jobId = UUID.randomUUID();
        String jobType = "ComplexDemoJob";
        JobDefinition jobDefinition = new JobDefinition(
                jobType,
                true,
                "requestType",
                "handlerClass",
                List.of(),
                List.of(),
                new JobSettings("Complex Demo Job", false, 3, List.of(), List.of(), "", "", "", "", "", "", "", null),
                false,
                null,
                List.of(),
                new JobDetailPage(null, "complex-demo-message-provider", "", true, true)
        );
        JobMessage message = new JobMessage(Instant.parse("2026-05-26T10:15:30Z"), JobMessageLevel.INFO, "hello", null);

        when(storageProvider.getJobById(jobId)).thenReturn(batchJob);
        when(batchJob.isBatchJob()).thenReturn(true);
        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(jobDefinition);
        when(jobDetailsProviderRegistry.findMessageProvider("complex-demo-message-provider")).thenReturn(Optional.of(jobMessageProvider));
        when(jobMessageProvider.searchJobMessages(jobId, JobMessageSearch.ALL, 0, 10))
                .thenReturn(new JobMessageProvider.PagedJobMessages(List.of(message), 1, 0, 10));

        GetJobDetailsMessageUseCase.MessagesPaginationResult result = useCase.execute(jobId, jobType, JobMessageSearch.ALL, 0, 10);

        assertThat(result.pageItems()).containsExactly(message);
        assertThat(result.pagination().totalElements()).isEqualTo(1);
        verify(jobMessageProvider).searchJobMessages(jobId, JobMessageSearch.ALL, 0, 10);
        verify(storageProvider, never()).getJobList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}

