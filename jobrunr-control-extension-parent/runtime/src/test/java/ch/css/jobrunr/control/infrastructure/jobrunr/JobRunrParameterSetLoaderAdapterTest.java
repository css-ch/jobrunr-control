package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSettings;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobRunrParameterSetLoaderAdapter")
class JobRunrParameterSetLoaderAdapterTest {

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private ParameterStoragePort parameterStoragePort;

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private Job job;

    @Mock
    private JobDetails jobDetails;

    private JobRunrParameterSetLoaderAdapter adapter;

    private static final String HANDLER_CLASS = "com.example.InlineJobHandler";
    private static final String SIMPLE_CLASS = "InlineJobHandler";

    @BeforeEach
    void setUp() {
        adapter = new JobRunrParameterSetLoaderAdapter(
                storageProvider, parameterStoragePort, jobDefinitionDiscoveryService);
    }

    @Test
    @DisplayName("should load inline parameters directly from job")
    void loadParameters_InlineParameters_ReturnsJobParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        JobDefinition inlineDef = new JobDefinition(
                SIMPLE_CLASS, false, "InlineJobRequest", HANDLER_CLASS,
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                false, null
        );

        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getJobDetails()).thenReturn(jobDetails);
        when(jobDetails.getClassName()).thenReturn(HANDLER_CLASS);
        when(jobDetails.getJobParameters()).thenReturn(List.of());
        when(jobDefinitionDiscoveryService.findJobByType(SIMPLE_CLASS)).thenReturn(Optional.of(inlineDef));

        // Act
        Map<String, Object> result = adapter.loadParameters(jobId);

        // Assert
        assertThat(result).isNotNull();
        verify(storageProvider).getJobById(jobId);
    }

    @Test
    @DisplayName("should load external parameters using job ID as parameter set ID")
    void loadParameters_ExternalParameters_LoadsByJobId() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Map<String, Object> expectedParams = Map.of("param1", "value1", "param2", 123);
        ParameterSet parameterSet = ParameterSet.create(jobId, "ExternalJob", expectedParams);
        JobDefinition externalDef = new JobDefinition(
                "ExternalJobHandler", false, "ExternalJobRequest", "com.example.ExternalJobHandler",
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                true, "parameterSetId"
        );

        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getJobDetails()).thenReturn(jobDetails);
        when(jobDetails.getClassName()).thenReturn("com.example.ExternalJobHandler");
        when(jobDefinitionDiscoveryService.findJobByType("ExternalJobHandler"))
                .thenReturn(Optional.of(externalDef));
        when(parameterStoragePort.findById(jobId)).thenReturn(Optional.of(parameterSet));

        // Act
        Map<String, Object> result = adapter.loadParameters(jobId);

        // Assert
        assertThat(result)
                .containsEntry("param1", "value1")
                .containsEntry("param2", 123);
        verify(parameterStoragePort).findById(jobId);
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
