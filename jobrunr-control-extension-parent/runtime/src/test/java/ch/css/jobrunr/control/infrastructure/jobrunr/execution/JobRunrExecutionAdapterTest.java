package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobStatus;
import ch.css.jobrunr.control.domain.ParameterSetLoaderPort;
import ch.css.jobrunr.control.infrastructure.jobrunr.ConfigurableJobSearchAdapter;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobWorkflowResolver;
import org.jobrunr.jobs.BatchJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobRunrExecutionAdapter")
class JobRunrExecutionAdapterTest {

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private ConfigurableJobSearchAdapter configurableJobSearchAdapter;

    @Mock
    private JobChainStatusEvaluator jobChainStatusEvaluator;

    @Mock
    private JobStateMapper jobStateMapper;

    @Mock
    private ParameterSetLoaderPort parameterSetLoaderPort;

    @Mock
    private JobWorkflowResolver jobWorkflowResolver;

    @InjectMocks
    private JobRunrExecutionAdapter adapter;

    @Test
    @DisplayName("should calculate history batch progress from processing workers across nested batches")
    void getJobExecutions_NestedBatch_UsesProcessingWorkersForHistoryProgress() {
        UUID rootJobId = UUID.randomUUID();
        BatchJob rootJob = mock(BatchJob.class);
        JobDefinition jobDefinition = mock(JobDefinition.class);
        SucceededState succeededState = mock(SucceededState.class);
        List<Job> processingJobs = List.of(
                processingJob(StateName.SUCCEEDED),
                processingJob(StateName.SUCCEEDED),
                processingJob(StateName.FAILED),
                processingJob(StateName.PROCESSING)
        );

        when(configurableJobSearchAdapter.getConfigurableJob(anyList())).thenReturn(List.of(
                new ConfigurableJobSearchAdapter.ConfigurableJobSearchResult(jobDefinition, rootJob)));
        when(jobDefinition.jobType()).thenReturn("RecapDemoJob");
        when(rootJob.getId()).thenReturn(rootJobId);
        when(rootJob.getJobName()).thenReturn("recap-demo");
        when(rootJob.getMetadata()).thenReturn(Map.of());
        when(rootJob.getJobStates()).thenReturn(List.of());
        when(rootJob.getJobState()).thenReturn(succeededState);
        when(rootJob.isBatchJob()).thenReturn(true);
        when(jobStateMapper.mapJobState(succeededState)).thenReturn(JobStatus.SUCCEEDED);
        when(parameterSetLoaderPort.loadParameters(rootJobId)).thenReturn(Map.of());
        when(jobWorkflowResolver.resolveProcessingJobs(rootJobId)).thenReturn(processingJobs);

        List<JobExecutionInfo> result = adapter.getJobExecutions();

        assertThat(result).singleElement().satisfies(execution ->
                assertThat(execution.getBatchProgress()).hasValueSatisfying(progress -> {
                    assertThat(progress.total()).isEqualTo(4);
                    assertThat(progress.succeeded()).isEqualTo(2);
                    assertThat(progress.failed()).isEqualTo(1);
                    assertThat(progress.getPending()).isEqualTo(1);
                }));
        verify(jobWorkflowResolver).resolveProcessingJobs(rootJobId);
    }

    private static Job processingJob(StateName state) {
        Job job = mock(Job.class);
        when(job.getState()).thenReturn(state);
        return job;
    }
}
