package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.JobRecapParameter;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.JobStatus;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultJobDetailsProvider")
class DefaultJobDetailsProviderTest {

    @Mock
    private JobExecutionPort jobExecutionPort;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private BatchJob batchJob;

    @Mock
    private Job childJobA;

    @Mock
    private Job childJobB;

    @Test
    @DisplayName("reuses one child scan for recap, message counter, and message page")
    void usesSharedSnapshotAcrossProviderMethods() {
        UUID batchId = UUID.randomUUID();
        DefaultJobDetailsProvider provider = new DefaultJobDetailsProvider(jobExecutionPort, storageProvider, jobDefinitionDiscoveryService);

        when(storageProvider.getJobById(batchId)).thenReturn(batchJob);
        when(batchJob.isBatchJob()).thenReturn(true);
        when(batchJob.getId()).thenReturn(batchId);
        when(storageProvider.getJobList(any(), any())).thenReturn(List.of(childJobA, childJobB));

        when(childJobA.getResult()).thenReturn(new RecapResult(2L));
        when(childJobB.getResult()).thenReturn(new RecapResult(3L));
        when(childJobA.getMetadata()).thenReturn(Map.of());
        when(childJobB.getMetadata()).thenReturn(Map.of());
        when(childJobA.getLastJobStateOfType(any())).thenReturn(Optional.empty());
        when(childJobB.getLastJobStateOfType(any())).thenReturn(Optional.empty());

        JobExecutionInfo executionInfo = new JobExecutionInfo(
                batchId,
                "Demo",
                "DemoJob",
                JobStatus.SUCCEEDED,
                Instant.parse("2026-05-28T11:55:00Z"),
                Instant.parse("2026-05-28T11:59:00Z"),
                null,
                Map.of(),
                Map.of(),
                null,
                null
        );
        when(jobExecutionPort.getJobExecutionById(batchId)).thenReturn(Optional.of(executionInfo));
        when(jobDefinitionDiscoveryService.requireJobByType("DemoJob")).thenReturn(jobDefinition());

        Map<String, Long> recap = provider.determineRecap(batchId);
        JobMessageCounter messageCounter = provider.determineJobMessageCounter(batchId);
        JobMessageProvider.PagedJobMessages messages = provider.searchJobMessages(batchId, JobMessageSearch.ALL, 0, 10);

        assertThat(recap).containsEntry("processed", 5L);
        assertThat(messageCounter.totalMessages()).isZero();
        assertThat(messages.items()).isEmpty();
        assertThat(messages.totalItems()).isZero();

        verify(storageProvider, times(1)).getJobList(any(), any());
        verify(jobExecutionPort, times(1)).getJobExecutionById(batchId);
        verify(jobDefinitionDiscoveryService, times(1)).requireJobByType("DemoJob");
    }

    @Test
    @DisplayName("rebuilds snapshot after ttl expires")
    void rebuildsSnapshotAfterTtlExpiration() throws InterruptedException {
        UUID batchId = UUID.randomUUID();
        DefaultJobDetailsProvider provider = new DefaultJobDetailsProvider(jobExecutionPort, storageProvider, jobDefinitionDiscoveryService);

        when(storageProvider.getJobById(batchId)).thenReturn(batchJob);
        when(batchJob.isBatchJob()).thenReturn(true);
        when(batchJob.getId()).thenReturn(batchId);
        when(storageProvider.getJobList(any(), any())).thenReturn(List.of(childJobA));

        when(childJobA.getResult()).thenReturn(new RecapResult(1L));
        when(childJobA.getMetadata()).thenReturn(Map.of());
        when(childJobA.getLastJobStateOfType(any())).thenReturn(Optional.empty());

        JobExecutionInfo executionInfo = new JobExecutionInfo(
                batchId,
                "Demo",
                "DemoJob",
                JobStatus.SUCCEEDED,
                Instant.parse("2026-05-28T11:50:00Z"),
                Instant.parse("2026-05-28T11:55:00Z"),
                null,
                Map.of(),
                Map.of(),
                null,
                null
        );
        when(jobExecutionPort.getJobExecutionById(batchId)).thenReturn(Optional.of(executionInfo));
        when(jobDefinitionDiscoveryService.requireJobByType("DemoJob")).thenReturn(jobDefinition());

        provider.determineRecap(batchId);

        Thread.sleep(2100L);
        provider.determineJobMessageCounter(batchId);

        verify(storageProvider, times(2)).getJobList(any(), any());
        verify(jobExecutionPort, times(2)).getJobExecutionById(eq(batchId));
    }

    private JobDefinition jobDefinition() {
        return new JobDefinition(
                "DemoJob",
                true,
                "requestType",
                "handlerClass",
                List.of(),
                List.of(),
                new JobSettings("Demo Job", false, 3, List.of(), List.of(), "", "", "", "", "", "", "", null),
                false,
                null,
                List.of(new JobRecapParameter("processed", "Processed", "", "", "", 0)),
                null
        );
    }

    private record RecapResult(long processed) {
    }

}

