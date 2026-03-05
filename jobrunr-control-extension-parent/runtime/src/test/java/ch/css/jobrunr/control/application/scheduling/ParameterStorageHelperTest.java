package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.testutils.JobDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParameterStorageHelper")
class ParameterStorageHelperTest {

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @Mock
    private ParameterStorageService parameterStorageService;

    @InjectMocks
    private ParameterStorageHelper helper;

    private JobDefinition inlineJob;
    private JobDefinition externalJob;

    @BeforeEach
    void setUp() {
        inlineJob = new JobDefinitionBuilder().withJobType("InlineJob").build();
        externalJob = new JobDefinitionBuilder().withJobType("ExternalJob").withExternalParameters().build();
    }

    // -------------------------------------------------------------------------
    // scheduleJobWithParameters
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("scheduleJobWithParameters — inline: single-phase, passes converted params directly")
    void scheduleJobWithParameters_InlineParams_SinglePhase() {
        String jobType = "InlineJob";
        String jobName = "My Job";
        Map<String, Object> convertedParams = Map.of("key", "value");
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        UUID expectedId = UUID.randomUUID();

        when(jobSchedulerPort.scheduleJob(inlineJob, jobName, convertedParams, false, scheduledAt, null))
                .thenReturn(expectedId);

        UUID result = helper.scheduleJobWithParameters(inlineJob, jobType, jobName, convertedParams, false, scheduledAt, null);

        assertThat(result).isEqualTo(expectedId);
        verify(jobSchedulerPort).scheduleJob(inlineJob, jobName, convertedParams, false, scheduledAt, null);
        verify(jobSchedulerPort, never()).updateJobParameters(any(), any());
        verifyNoInteractions(parameterStorageService);
    }

    @Test
    @DisplayName("scheduleJobWithParameters — external: two-phase, stores params and calls updateJobParameters")
    void scheduleJobWithParameters_ExternalParams_TwoPhase() {
        String jobType = "ExternalJob";
        String jobName = "My Job";
        Map<String, Object> convertedParams = Map.of("key", "value");
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        UUID jobId = UUID.randomUUID();

        when(parameterStorageService.isExternalStorageAvailable()).thenReturn(true);
        when(jobSchedulerPort.scheduleJob(eq(externalJob), eq(jobName), eq(Map.of()), eq(false), eq(scheduledAt), isNull()))
                .thenReturn(jobId);

        UUID result = helper.scheduleJobWithParameters(externalJob, jobType, jobName, convertedParams, false, scheduledAt, null);

        assertThat(result).isEqualTo(jobId);

        // Phase 1: job created with empty params
        verify(jobSchedulerPort).scheduleJob(externalJob, jobName, Map.of(), false, scheduledAt, null);

        // Phase 2: parameters stored with job UUID
        verify(parameterStorageService).store(argThat(ps -> ps.id().equals(jobId) && ps.parameters().equals(convertedParams)));

        // Phase 3: job updated with empty param map
        verify(jobSchedulerPort).updateJobParameters(jobId, Map.of());
    }

    @Test
    @DisplayName("scheduleJobWithParameters — external: phases execute in correct order")
    void scheduleJobWithParameters_ExternalParams_CorrectOrder() {
        UUID jobId = UUID.randomUUID();
        when(parameterStorageService.isExternalStorageAvailable()).thenReturn(true);
        when(jobSchedulerPort.scheduleJob(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(jobId);

        helper.scheduleJobWithParameters(externalJob, "ExternalJob", "Job", Map.of("x", 1), false, null, null);

        InOrder order = inOrder(jobSchedulerPort, parameterStorageService);
        order.verify(jobSchedulerPort).scheduleJob(any(), any(), eq(Map.of()), anyBoolean(), any(), any());
        order.verify(parameterStorageService).store(any());
        order.verify(jobSchedulerPort).updateJobParameters(eq(jobId), eq(Map.of()));
    }

    @Test
    @DisplayName("scheduleJobWithParameters — external: throws when external storage not available")
    void scheduleJobWithParameters_ExternalParams_StorageNotAvailable_Throws() {
        when(parameterStorageService.isExternalStorageAvailable()).thenReturn(false);

        assertThatThrownBy(() ->
                helper.scheduleJobWithParameters(externalJob, "ExternalJob", "Job", Map.of(), false, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external parameter storage");

        verifyNoInteractions(jobSchedulerPort);
    }

    @Test
    @DisplayName("scheduleJobWithParameters — passes labels to scheduleJob")
    void scheduleJobWithParameters_PassesLabels() {
        List<String> labels = List.of("template");
        UUID expectedId = UUID.randomUUID();
        when(jobSchedulerPort.scheduleJob(any(), any(), any(), anyBoolean(), any(), eq(labels))).thenReturn(expectedId);

        UUID result = helper.scheduleJobWithParameters(inlineJob, "InlineJob", "Job", Map.of(), true, null, labels);

        assertThat(result).isEqualTo(expectedId);
        verify(jobSchedulerPort).scheduleJob(inlineJob, "Job", Map.of(), true, null, labels);
    }

    // -------------------------------------------------------------------------
    // updateJobWithParameters
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateJobWithParameters — inline: single-phase, passes converted params directly")
    void updateJobWithParameters_InlineParams_SinglePhase() {
        UUID jobId = UUID.randomUUID();
        Map<String, Object> convertedParams = Map.of("key", "updated");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        helper.updateJobWithParameters(jobId, inlineJob, "InlineJob", "Job", convertedParams, false, scheduledAt, null);

        verify(jobSchedulerPort).updateJob(jobId, inlineJob, "Job", convertedParams, false, scheduledAt, null);
        verifyNoInteractions(parameterStorageService);
    }

    @Test
    @DisplayName("updateJobWithParameters — external: updates param set in-place, then updates job with empty map")
    void updateJobWithParameters_ExternalParams_UpdatesInPlace() {
        UUID jobId = UUID.randomUUID();
        Map<String, Object> convertedParams = Map.of("key", "updated");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(parameterStorageService.isExternalStorageAvailable()).thenReturn(true);

        helper.updateJobWithParameters(jobId, externalJob, "ExternalJob", "Job", convertedParams, false, scheduledAt, null);

        verify(parameterStorageService).update(argThat(ps -> ps.id().equals(jobId) && ps.parameters().equals(convertedParams)));
        verify(jobSchedulerPort).updateJob(jobId, externalJob, "Job", Map.of(), false, scheduledAt, null);
    }

    @Test
    @DisplayName("updateJobWithParameters — external: throws when external storage not available")
    void updateJobWithParameters_ExternalParams_StorageNotAvailable_Throws() {
        when(parameterStorageService.isExternalStorageAvailable()).thenReturn(false);

        assertThatThrownBy(() ->
                helper.updateJobWithParameters(UUID.randomUUID(), externalJob, "ExternalJob", "Job", Map.of(), false, Instant.now(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external parameter storage");

        verifyNoInteractions(jobSchedulerPort);
    }

    @Test
    @DisplayName("updateJobWithParameters — passes labels to updateJob")
    void updateJobWithParameters_PassesLabels() {
        UUID jobId = UUID.randomUUID();
        List<String> labels = List.of("template");
        Instant scheduledAt = Instant.now();

        helper.updateJobWithParameters(jobId, inlineJob, "InlineJob", "Job", Map.of(), false, scheduledAt, labels);

        verify(jobSchedulerPort).updateJob(jobId, inlineJob, "Job", Map.of(), false, scheduledAt, labels);
    }
}
