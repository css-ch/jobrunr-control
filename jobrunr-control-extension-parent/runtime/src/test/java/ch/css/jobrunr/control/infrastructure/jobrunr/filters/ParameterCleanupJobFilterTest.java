package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParameterCleanupJobFilter")
class ParameterCleanupJobFilterTest {

    @Mock
    private ParameterStoragePort parameterStoragePort;

    @Mock(lenient = true)
    private Job job;

    @Mock(lenient = true)
    private JobDetails jobDetails;

    @Mock(lenient = true)
    private JobState deletedState;

    @Mock(lenient = true)
    private JobState succeededState;

    @Mock(lenient = true)
    private JobState processingState;

    private ParameterCleanupJobFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ParameterCleanupJobFilter(parameterStoragePort);

        // Configure state mocks
        when(deletedState.getName()).thenReturn(StateName.DELETED);
        when(succeededState.getName()).thenReturn(StateName.SUCCEEDED);
        when(processingState.getName()).thenReturn(StateName.PROCESSING);
    }

    @Test
    @DisplayName("should cleanup parameters when job is deleted")
    void onStateApplied_JobDeleted_CleanupsParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID parameterSetId = UUID.randomUUID();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("__parameterSetId", parameterSetId.toString());

        when(job.getId()).thenReturn(jobId);
        when(job.getMetadata()).thenReturn(metadata);
        when(job.getJobDetails()).thenReturn(jobDetails);

        // Act
        filter.onStateApplied(job, null, deletedState);

        // Assert
        verify(parameterStoragePort).deleteById(parameterSetId);
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
    @DisplayName("should do nothing when job has no parameter set ID")
    void onStateApplied_NoParameterSetId_DoesNotCleanup() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Map<String, Object> metadata = new HashMap<>();

        when(job.getId()).thenReturn(jobId);
        when(job.getMetadata()).thenReturn(metadata);
        when(job.getJobDetails()).thenReturn(jobDetails);
        when(jobDetails.getJobParameters()).thenReturn(java.util.List.of());

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
        UUID parameterSetId = UUID.randomUUID();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("__parameterSetId", parameterSetId.toString());

        when(job.getId()).thenReturn(jobId);
        when(job.getMetadata()).thenReturn(metadata);
        when(job.getJobDetails()).thenReturn(jobDetails);

        doThrow(new RuntimeException("Storage error")).when(parameterStoragePort).deleteById(parameterSetId);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> filter.onStateApplied(job, null, deletedState));
    }

    @Test
    @DisplayName("should parse parameter set ID from string")
    void onStateApplied_ParameterSetIdAsString_ParsesAndDeletes() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID parameterSetId = UUID.randomUUID();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("__parameterSetId", parameterSetId.toString());

        when(job.getId()).thenReturn(jobId);
        when(job.getMetadata()).thenReturn(metadata);
        when(job.getJobDetails()).thenReturn(jobDetails);

        // Act
        filter.onStateApplied(job, null, deletedState);

        // Assert
        verify(parameterStoragePort).deleteById(parameterSetId);
    }

    @Test
    @DisplayName("should handle null metadata gracefully")
    void onStateApplied_NullMetadata_DoesNotThrow() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        when(job.getId()).thenReturn(jobId);
        when(job.getMetadata()).thenReturn(null);
        when(job.getJobDetails()).thenReturn(jobDetails);
        when(jobDetails.getJobParameters()).thenReturn(java.util.List.of());

        // Act & Assert
        assertDoesNotThrow(() -> filter.onStateApplied(job, null, deletedState));
        verify(parameterStoragePort, never()).deleteById(any());
    }
}
