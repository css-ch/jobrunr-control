package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ParameterCleanupJobFilter")
class ParameterCleanupJobFilterTest {

    @Mock
    private ParameterStoragePort parameterStoragePort;

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private Job job;

    @Mock
    private JobDetails jobDetails;

    @Mock
    private JobState deletedState;

    @Mock
    private JobState succeededState;

    @Mock
    private JobState processingState;

    private ParameterCleanupJobFilter filter;

    private static final String HANDLER_CLASS = "com.example.ExternalParamJobHandler";
    private static final String SIMPLE_CLASS = "ExternalParamJobHandler";

    @BeforeEach
    void setUp() {
        filter = new ParameterCleanupJobFilter(parameterStoragePort, jobDefinitionDiscoveryService);

        when(deletedState.getName()).thenReturn(StateName.DELETED);
        when(succeededState.getName()).thenReturn(StateName.SUCCEEDED);
        when(processingState.getName()).thenReturn(StateName.PROCESSING);

        when(job.getJobDetails()).thenReturn(jobDetails);
        when(jobDetails.getClassName()).thenReturn(HANDLER_CLASS);
    }

    private JobDefinition externalJobDefinition() {
        return new JobDefinition(
                SIMPLE_CLASS, false, "TestJobRequest", HANDLER_CLASS,
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                true, "parameterSetId"
        );
    }

    @Test
    @DisplayName("should cleanup parameters when job is deleted, using job ID as parameter set ID")
    void onStateApplied_JobDeleted_CleanupsParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);
        when(jobDefinitionDiscoveryService.findJobByType(SIMPLE_CLASS))
                .thenReturn(Optional.of(externalJobDefinition()));

        // Act
        filter.onStateApplied(job, null, deletedState);

        // Assert
        verify(parameterStoragePort).deleteById(jobId);
    }

    @Test
    @DisplayName("should not cleanup when job transitions to non-deleted state")
    void onStateApplied_JobSucceeded_DoesNotCleanup() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);

        // Act
        filter.onStateApplied(job, null, succeededState);

        // Assert
        verify(parameterStoragePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("should not cleanup when job is processing")
    void onStateApplied_JobProcessing_DoesNotCleanup() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);

        // Act
        filter.onStateApplied(job, null, processingState);

        // Assert
        verify(parameterStoragePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("should do nothing when job type does not use external parameters")
    void onStateApplied_InlineJobType_DoesNotCleanup() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);
        JobDefinition inlineJobDef = new JobDefinition(
                SIMPLE_CLASS, false, "TestJobRequest", HANDLER_CLASS,
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                false, null
        );
        when(jobDefinitionDiscoveryService.findJobByType(SIMPLE_CLASS))
                .thenReturn(Optional.of(inlineJobDef));

        // Act
        filter.onStateApplied(job, null, deletedState);

        // Assert
        verify(parameterStoragePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("should do nothing when job type is not found in registry")
    void onStateApplied_UnknownJobType_DoesNotCleanup() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);
        when(jobDefinitionDiscoveryService.findJobByType(SIMPLE_CLASS)).thenReturn(Optional.empty());

        // Act
        filter.onStateApplied(job, null, deletedState);

        // Assert
        verify(parameterStoragePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("should handle cleanup errors gracefully without throwing")
    void onStateApplied_CleanupFails_DoesNotThrow() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);
        when(jobDefinitionDiscoveryService.findJobByType(SIMPLE_CLASS))
                .thenReturn(Optional.of(externalJobDefinition()));
        doThrow(new RuntimeException("Storage error")).when(parameterStoragePort).deleteById(jobId);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> filter.onStateApplied(job, null, deletedState));
    }
}
