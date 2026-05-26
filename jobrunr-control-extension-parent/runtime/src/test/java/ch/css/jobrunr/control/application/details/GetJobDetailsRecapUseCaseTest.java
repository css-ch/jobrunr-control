package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobDetailPage;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.JobRecapParameter;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.JobStatus;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetJobDetailsRecapUseCase")
class GetJobDetailsRecapUseCaseTest {

    @Mock
    private JobExecutionPort jobExecutionPort;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private JobDetailsProviderRegistry jobDetailsProviderRegistry;

    @Mock
    private JobMessageProvider jobMessageProvider;

    @Mock
    private JobRecapProvider jobRecapProvider;

    @Mock
    private BatchJob batchJob;

    @InjectMocks
    private GetJobDetailsRecapUseCase useCase;

    @Test
    @DisplayName("should use configured custom providers for message count and recap counters")
    void execute_CustomProvidersConfigured_UsesProviders() {
        UUID jobId = UUID.randomUUID();
        String jobType = "ComplexDemoJob";
        JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
                jobId,
                "Complex Demo",
                jobType,
                JobStatus.SUCCEEDED,
                Instant.parse("2026-05-26T10:00:00Z"),
                Instant.parse("2026-05-26T10:10:00Z"),
                null,
                Map.of(),
                Map.of(),
                null,
                null
        );
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
                List.of(new JobRecapParameter("processed", "Processed", "", "", "", 0)),
                new JobDetailPage(null, "complex-demo-message-provider", "complex-demo-recap-provider", true, false)
        );

        when(jobExecutionPort.getJobExecutionById(jobId)).thenReturn(Optional.of(jobExecutionInfo));
        when(storageProvider.getJobById(jobId)).thenReturn(batchJob);
        when(batchJob.isBatchJob()).thenReturn(true);
        when(batchJob.getBatchJobStats()).thenReturn(new BatchJob.BatchJobStats(10, 7, 1));
        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(jobDefinition);
        when(jobDetailsProviderRegistry.findMessageProvider("complex-demo-message-provider")).thenReturn(Optional.of(jobMessageProvider));
        when(jobDetailsProviderRegistry.findRecapProvider("complex-demo-recap-provider")).thenReturn(Optional.of(jobRecapProvider));
        when(jobMessageProvider.determineJobMessageCounter(jobId, jobType)).thenReturn(new JobMessageCounter(12, 5, 4, 3));
        when(jobRecapProvider.determineRecap(jobId, jobType)).thenReturn(Map.of("processed", 42L));

        GetJobDetailsRecapUseCase.Result result = useCase.execute(jobId, jobType);

        assertThat(result.messageCount().totalMessages()).isEqualTo(12);
        assertThat(result.messageCount().warningMessages()).isEqualTo(4);
        assertThat(result.childJobCounters().totalChildJobs()).isEqualTo(10);
        assertThat(result.childJobCounters().succeededChildJobCount()).isEqualTo(7);
        assertThat(result.recapCounters().recapCounters()).containsEntry("processed", 42L);
        verify(jobMessageProvider).determineJobMessageCounter(jobId, jobType);
        verify(jobRecapProvider).determineRecap(jobId, jobType);
        verify(storageProvider, never()).getJobList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
