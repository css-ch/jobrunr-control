package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobDetailPage;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.JobRecapParameter;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.JobStatus;
import ch.css.jobrunr.control.domain.JobWorkflowPort;
import ch.css.jobrunr.control.domain.details.JobDetailsProviderRegistry;
import ch.css.jobrunr.control.domain.details.JobMessageAttemptFilter;
import ch.css.jobrunr.control.domain.details.JobMessageLevelCounters;
import ch.css.jobrunr.control.domain.details.JobMessageProvider;
import ch.css.jobrunr.control.domain.details.JobRecapProvider;
import org.jobrunr.jobs.Job;
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
import static org.mockito.Mockito.mock;
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
    private JobWorkflowPort jobWorkflowPort;

    @Mock
    private JobMessageProvider jobMessageProvider;

    @Mock
    private JobRecapProvider jobRecapProvider;

    @InjectMocks
    private GetJobDetailsRecapUseCase useCase;

    @Test
    @DisplayName("should use configured custom providers for message count and recap counters")
    void execute_CustomProvidersConfigured_UsesProviders() {
        UUID jobId = UUID.randomUUID();
        String jobType = "RecapDemoJob";
        JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
                jobId,
                "Recap Demo",
                jobType,
                JobStatus.SUCCEEDED,
                Instant.parse("2026-05-26T10:00:00Z"),
                Instant.parse("2026-05-26T10:10:00Z"),
                new BatchProgress(10, 7, 1),
                Map.of(),
                Map.of(),
                null,
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
                new JobSettings("Recap Demo Job", false, 3, List.of(), List.of(), "", "", "", "", "", "", "", null),
                false,
                null,
                List.of(new JobRecapParameter("processed", "Processed", "", "", "", "Ops", 0)),
                new JobDetailPage(null, "recap-demo-message-provider", "recap-demo-recap-provider", true, false)
        );
        when(jobExecutionPort.getJobExecutionById(jobId)).thenReturn(Optional.of(jobExecutionInfo));
        when(jobWorkflowPort.resolveProcessingJobProgress(jobId)).thenReturn(new BatchProgress(10, 7, 1));
        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(jobDefinition);
        when(jobDetailsProviderRegistry.getMessageProvider("recap-demo-message-provider")).thenReturn(jobMessageProvider);
        when(jobDetailsProviderRegistry.getRecapProvider("recap-demo-recap-provider")).thenReturn(jobRecapProvider);
        when(jobMessageProvider.determineJobMessageCounter(jobId, JobMessageAttemptFilter.LATEST_ONLY))
                .thenReturn(new JobMessageLevelCounters(12, 5, 4, 3));
        when(jobRecapProvider.determineRecap(jobId)).thenReturn(Map.of("processed", 42L));

        GetJobDetailsRecapUseCase.Result result = useCase.execute(jobId);

        assertThat(result.messageCount().totalMessages()).isEqualTo(24);
        assertThat(result.messageCount().infoMessages()).isEqualTo(12);
        assertThat(result.messageCount().warningMessages()).isEqualTo(5);
        assertThat(result.messageCount().errorMessages()).isEqualTo(4);
        assertThat(result.messageCount().exceptionMessages()).isEqualTo(3);
        assertThat(result.childJobCounters().totalChildJobs()).isEqualTo(10);
        assertThat(result.childJobCounters().succeededChildJobCount()).isEqualTo(7);
        assertThat(result.recapView().recapSections()).isNotEmpty();
        verify(jobMessageProvider).determineJobMessageCounter(jobId, JobMessageAttemptFilter.LATEST_ONLY);
        verify(jobRecapProvider).determineRecap(jobId);
        verify(storageProvider, never()).getJobList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should place grouped sections by first order and keep ungrouped parameters in global order")
    void execute_RecapSectionsFollowFirstOrderAndKeepUngroupedUngrouped() {
        UUID jobId = UUID.randomUUID();
        String jobType = "RecapDemoJob";
        JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
                jobId,
                "Recap Demo",
                jobType,
                JobStatus.SUCCEEDED,
                Instant.parse("2026-05-26T10:00:00Z"),
                Instant.parse("2026-05-26T10:10:00Z"),
                new BatchProgress(10, 7, 1),
                Map.of(),
                Map.of(),
                null,
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
                new JobSettings("Recap Demo Job", false, 3, List.of(), List.of(), "", "", "", "", "", "", "", null),
                false,
                null,
                List.of(
                        new JobRecapParameter("u1", "Ungrouped A", "", "", "", "", 1),
                        new JobRecapParameter("u2", "Ungrouped B", "", "", "", "", 2),
                        new JobRecapParameter("g1", "Verarbeitet", "", "", "", "Verarbeitung", 3),
                        new JobRecapParameter("u3", "Ungrouped C", "", "", "", "", 4),
                        new JobRecapParameter("e1", "Fehler Typ A", "", "", "", "Fehler", 5),
                        new JobRecapParameter("e2", "Fehler Typ B", "", "", "", "Fehler", 6),
                        new JobRecapParameter("g2", "Uebersprungen", "", "", "", "Verarbeitung", 7),
                        new JobRecapParameter("u4", "Ungrouped D", "", "", "", "", 10)
                ),
                new JobDetailPage(null, "recap-demo-message-provider", "recap-demo-recap-provider", true, false)
        );
        when(jobExecutionPort.getJobExecutionById(jobId)).thenReturn(Optional.of(jobExecutionInfo));
        when(jobWorkflowPort.resolveProcessingJobProgress(jobId)).thenReturn(new BatchProgress(10, 7, 1));
        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(jobDefinition);
        when(jobDetailsProviderRegistry.getMessageProvider("recap-demo-message-provider")).thenReturn(jobMessageProvider);
        when(jobDetailsProviderRegistry.getRecapProvider("recap-demo-recap-provider")).thenReturn(jobRecapProvider);
        when(jobMessageProvider.determineJobMessageCounter(jobId, JobMessageAttemptFilter.LATEST_ONLY))
                .thenReturn(new JobMessageLevelCounters(2, 1, 1, 0));
        when(jobRecapProvider.determineRecap(jobId)).thenReturn(Map.of(
                "u1", 5L,
                "u2", 3L,
                "g1", 30L,
                "u3", 9L,
                "e1", 4L,
                "e2", 3L,
                "g2", 12L,
                "u4", 9L
        ));

        GetJobDetailsRecapUseCase.Result result = useCase.execute(jobId);

        List<GetJobDetailsRecapUseCase.RecapSection> sections = result.recapView().recapSections();
        assertThat(sections).hasSize(6);

        assertThat(sections.getFirst().hasSection()).isFalse();
        assertThat(sections.getFirst().recapEntries()).hasSize(1);
        assertThat(sections.getFirst().recapEntries().getFirst().recapParameter().name()).isEqualTo("u1");

        assertThat(sections.get(1).hasSection()).isFalse();
        assertThat(sections.get(1).recapEntries().getFirst().recapParameter().name()).isEqualTo("u2");

        assertThat(sections.get(2).hasSection()).isTrue();
        assertThat(sections.get(2).sectionName()).isEqualTo("Verarbeitung");
        assertThat(sections.get(2).sectionTotal()).isEqualTo(42L);
        assertThat(sections.get(2).recapEntries())
                .extracting(entry -> entry.recapParameter().name())
                .containsExactly("g1", "g2");

        assertThat(sections.get(3).hasSection()).isFalse();
        assertThat(sections.get(3).recapEntries().getFirst().recapParameter().name()).isEqualTo("u3");

        assertThat(sections.get(4).hasSection()).isTrue();
        assertThat(sections.get(4).sectionName()).isEqualTo("Fehler");
        assertThat(sections.get(4).sectionTotal()).isEqualTo(7L);
        assertThat(sections.get(4).recapEntries())
                .extracting(entry -> entry.recapParameter().name())
                .containsExactly("e1", "e2");

        assertThat(sections.get(5).hasSection()).isFalse();
        assertThat(sections.get(5).recapEntries().getFirst().recapParameter().name()).isEqualTo("u4");
    }

    @Test
    @DisplayName("should count a succeeded non-batch job as one completed worker")
    void execute_SucceededNonBatchJob_ReturnsCompletedWorkerCounters() {
        UUID jobId = UUID.randomUUID();
        JobExecutionInfo jobExecutionInfo = createExecutionInfo(jobId, JobStatus.SUCCEEDED);
        JobDefinition jobDefinition = createJobDefinition();
        configureDefaultProviders(jobId, jobExecutionInfo, jobDefinition);

        GetJobDetailsRecapUseCase.Result result = useCase.execute(jobId);

        assertThat(result.childJobCounters())
                .extracting(
                        GetJobDetailsRecapUseCase.ChildJobCounters::totalChildJobs,
                        GetJobDetailsRecapUseCase.ChildJobCounters::succeededChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::failedChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::inProgressChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::completedPercentage
                )
                .containsExactly(1L, 1L, 0L, 0L, 100L);
        verify(jobWorkflowPort, never()).resolveProcessingJobProgress(jobId);
    }

    @Test
    @DisplayName("should count a failed non-batch job as one failed worker")
    void execute_FailedNonBatchJob_ReturnsFailedWorkerCounters() {
        UUID jobId = UUID.randomUUID();
        JobExecutionInfo jobExecutionInfo = createExecutionInfo(jobId, JobStatus.FAILED);
        JobDefinition jobDefinition = createJobDefinition();
        configureDefaultProviders(jobId, jobExecutionInfo, jobDefinition);
        Job job = mock(Job.class);
        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getUpdatedAt()).thenReturn(Instant.parse("2026-05-26T10:10:00Z"));

        GetJobDetailsRecapUseCase.Result result = useCase.execute(jobId);

        assertThat(result.childJobCounters())
                .extracting(
                        GetJobDetailsRecapUseCase.ChildJobCounters::totalChildJobs,
                        GetJobDetailsRecapUseCase.ChildJobCounters::succeededChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::failedChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::inProgressChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::completedPercentage
                )
                .containsExactly(1L, 0L, 1L, 0L, 0L);
        verify(jobWorkflowPort, never()).resolveProcessingJobProgress(jobId);
    }

    @Test
    @DisplayName("should count a processing non-batch job as one pending worker")
    void execute_ProcessingNonBatchJob_ReturnsPendingWorkerCounters() {
        UUID jobId = UUID.randomUUID();
        JobExecutionInfo jobExecutionInfo = createExecutionInfo(jobId, JobStatus.PROCESSING);
        JobDefinition jobDefinition = createJobDefinition();
        configureDefaultProviders(jobId, jobExecutionInfo, jobDefinition);

        GetJobDetailsRecapUseCase.Result result = useCase.execute(jobId);

        assertThat(result.childJobCounters())
                .extracting(
                        GetJobDetailsRecapUseCase.ChildJobCounters::totalChildJobs,
                        GetJobDetailsRecapUseCase.ChildJobCounters::succeededChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::failedChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::inProgressChildJobCount,
                        GetJobDetailsRecapUseCase.ChildJobCounters::completedPercentage
                )
                .containsExactly(1L, 0L, 0L, 1L, 0L);
        verify(jobWorkflowPort, never()).resolveProcessingJobProgress(jobId);
    }

    private void configureDefaultProviders(UUID jobId, JobExecutionInfo jobExecutionInfo, JobDefinition jobDefinition) {
        when(jobExecutionPort.getJobExecutionById(jobId)).thenReturn(Optional.of(jobExecutionInfo));
        when(jobDefinitionDiscoveryService.requireJobByType(jobExecutionInfo.getJobType())).thenReturn(jobDefinition);
        when(jobDetailsProviderRegistry.getMessageProvider(null)).thenReturn(jobMessageProvider);
        when(jobDetailsProviderRegistry.getRecapProvider(null)).thenReturn(jobRecapProvider);
        when(jobMessageProvider.determineJobMessageCounter(jobId, JobMessageAttemptFilter.LATEST_ONLY))
                .thenReturn(new JobMessageLevelCounters(0, 0, 0, 0));
        when(jobRecapProvider.determineRecap(jobId)).thenReturn(Map.of());
    }

    private static JobExecutionInfo createExecutionInfo(UUID jobId, JobStatus status) {
        return new JobExecutionInfo(
                jobId,
                "Worker",
                "WorkerJob",
                status,
                Instant.parse("2026-05-26T10:00:00Z"),
                status == JobStatus.SUCCEEDED ? Instant.parse("2026-05-26T10:10:00Z") : null,
                null,
                Map.of(),
                Map.of(),
                null,
                null,
                null
        );
    }

    private static JobDefinition createJobDefinition() {
        return new JobDefinition(
                "WorkerJob",
                false,
                "requestType",
                "handlerClass",
                List.of(),
                List.of(),
                new JobSettings("Worker Job", false, 3, List.of(), List.of(), "", "", "", "", "", "", "", null),
                false,
                null,
                List.of(),
                new JobDetailPage(null, null, null, false, false)
        );
    }

}
