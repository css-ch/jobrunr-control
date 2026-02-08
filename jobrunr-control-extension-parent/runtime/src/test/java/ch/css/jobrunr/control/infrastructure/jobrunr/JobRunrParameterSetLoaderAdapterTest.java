package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.exceptions.ParameterSetNotFoundException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobRunrParameterSetLoaderAdapter")
class JobRunrParameterSetLoaderAdapterTest {

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private ParameterStoragePort parameterStoragePort;

    @Mock
    private Job job;

    @Mock
    private JobDetails jobDetails;

    private JobRunrParameterSetLoaderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JobRunrParameterSetLoaderAdapter(storageProvider, parameterStoragePort);
    }

    @Test
    @DisplayName("should load inline parameters directly from job")
    void loadParameters_InlineParameters_ReturnsJobParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getJobDetails()).thenReturn(jobDetails);
        when(jobDetails.getJobParameters()).thenReturn(List.of());

        // Act
        Map<String, Object> result = adapter.loadParameters(jobId);

        // Assert
        assertThat(result).isNotNull();
        verify(storageProvider).getJobById(jobId);
    }

    @Test
    @DisplayName("should load parameters by set ID")
    void loadParametersBySetId_ValidId_ReturnsParameters() {
        // Arrange
        UUID parameterSetId = UUID.randomUUID();
        Map<String, Object> expectedParams = Map.of("param1", "value1", "param2", 123);
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, "TestJob", expectedParams);

        when(parameterStoragePort.findById(parameterSetId)).thenReturn(Optional.of(parameterSet));

        // Act
        Map<String, Object> result = adapter.loadParametersBySetId(parameterSetId);

        // Assert
        assertThat(result)
                .isNotNull()
                .containsEntry("param1", "value1")
                .containsEntry("param2", 123);
    }

    @Test
    @DisplayName("should update last accessed timestamp when loading parameters")
    void loadParametersBySetId_ValidId_UpdatesLastAccessed() {
        // Arrange
        UUID parameterSetId = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, "TestJob", Map.of());

        when(parameterStoragePort.findById(parameterSetId)).thenReturn(Optional.of(parameterSet));

        // Act
        adapter.loadParametersBySetId(parameterSetId);

        // Assert
        verify(parameterStoragePort).updateLastAccessed(parameterSetId);
    }

    @Test
    @DisplayName("should throw ParameterSetNotFoundException when parameter set not found")
    void loadParametersBySetId_NotFound_ThrowsException() {
        // Arrange
        UUID parameterSetId = UUID.randomUUID();
        when(parameterStoragePort.findById(parameterSetId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adapter.loadParametersBySetId(parameterSetId))
                .isInstanceOf(ParameterSetNotFoundException.class);
    }

    @Test
    @DisplayName("should not update last accessed when parameter set not found")
    void loadParametersBySetId_NotFound_DoesNotUpdateLastAccessed() {
        // Arrange
        UUID parameterSetId = UUID.randomUUID();
        when(parameterStoragePort.findById(parameterSetId)).thenReturn(Optional.empty());

        // Act & Assert
        try {
            adapter.loadParametersBySetId(parameterSetId);
        } catch (ParameterSetNotFoundException e) {
            // Expected
        }

        verify(parameterStoragePort, never()).updateLastAccessed(any());
    }

    @Test
    @DisplayName("should handle complex parameter types")
    void loadParametersBySetId_ComplexTypes_ReturnsCorrectly() {
        // Arrange
        UUID parameterSetId = UUID.randomUUID();
        Map<String, Object> complexParams = Map.of(
                "string", "value",
                "number", 42,
                "boolean", true,
                "list", List.of("a", "b", "c")
        );
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, "ComplexJob", complexParams);

        when(parameterStoragePort.findById(parameterSetId)).thenReturn(Optional.of(parameterSet));

        // Act
        Map<String, Object> result = adapter.loadParametersBySetId(parameterSetId);

        // Assert
        assertThat(result)
                .containsEntry("string", "value")
                .containsEntry("number", 42)
                .containsEntry("boolean", true)
                .containsEntry("list", List.of("a", "b", "c"));
    }
}
