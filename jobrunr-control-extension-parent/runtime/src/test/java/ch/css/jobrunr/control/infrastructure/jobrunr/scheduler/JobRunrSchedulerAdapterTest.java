package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.JobSearchRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JobRunrSchedulerAdapter")
class JobRunrSchedulerAdapterTest {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private JobInvoker jobInvoker;

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    private JobRunrSchedulerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JobRunrSchedulerAdapter(jobScheduler, storageProvider, jobInvoker, jobDefinitionDiscoveryService);
    }

    @Test
    @DisplayName("getScheduledJobs skips jobs whose handler has no @ConfigurableJob definition")
    void getScheduledJobs_skipsHandlersWithoutConfigurableJob() {
        Job configurableJob = mockJob(UUID.randomUUID(), "com.example.ConfigurableHandler", "Configurable");
        Job adHocJob = mockJob(UUID.randomUUID(), "com.example.AdHocHandler", "AdHoc");

        when(storageProvider.getJobList(any(JobSearchRequest.class), any(AmountRequest.class)))
                .thenReturn(List.of(configurableJob, adHocJob));
        when(jobDefinitionDiscoveryService.findJobByHandlerClassName("com.example.ConfigurableHandler"))
                .thenReturn(Optional.of(jobDefinition("ConfigurableHandler", "com.example.ConfigurableHandler")));
        when(jobDefinitionDiscoveryService.findJobByHandlerClassName("com.example.AdHocHandler"))
                .thenReturn(Optional.empty());

        List<ScheduledJobInfo> result = adapter.getScheduledJobs();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getJobDefinition().jobType()).isEqualTo("ConfigurableHandler");
    }

    @Test
    @DisplayName("getScheduledJobById returns null when handler has no @ConfigurableJob definition")
    void getScheduledJobById_returnsNullWhenDefinitionMissing() {
        UUID jobId = UUID.randomUUID();
        Job job = mockJob(jobId, "com.example.AdHocHandler", "AdHoc");

        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(jobDefinitionDiscoveryService.findJobByHandlerClassName("com.example.AdHocHandler"))
                .thenReturn(Optional.empty());

        ScheduledJobInfo result = adapter.getScheduledJobById(jobId);

        assertThat(result).isNull();
    }

    private Job mockJob(UUID id, String handlerClassName, String jobName) {
        Job job = org.mockito.Mockito.mock(Job.class);
        JobDetails jobDetails = org.mockito.Mockito.mock(JobDetails.class);
        when(job.getId()).thenReturn(id);
        when(job.getJobName()).thenReturn(jobName);
        when(job.getJobDetails()).thenReturn(jobDetails);
        when(job.getState()).thenReturn(StateName.SCHEDULED);
        when(job.getJobStates()).thenReturn(List.of());
        when(job.getCreatedAt()).thenReturn(Instant.now());
        when(job.getLabels()).thenReturn(List.of());
        when(jobDetails.getClassName()).thenReturn(handlerClassName);
        when(jobDetails.getJobParameters()).thenReturn(List.of());
        return job;
    }

    private JobDefinition jobDefinition(String simpleName, String fqcn) {
        return new JobDefinition(
                simpleName,
                false,
                "com.example.TestRequest",
                fqcn,
                List.of(),
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null, null),
                false,
                null
        );
    }
}
