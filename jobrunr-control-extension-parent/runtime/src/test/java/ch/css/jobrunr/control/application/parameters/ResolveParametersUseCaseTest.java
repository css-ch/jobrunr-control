package ch.css.jobrunr.control.application.parameters;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveParametersUseCase")
class ResolveParametersUseCaseTest {

    @Mock
    private ParameterStorageService parameterStorageService;

    @InjectMocks
    private ResolveParametersUseCase useCase;

    private static ScheduledJobInfo inlineJobInfo(UUID jobId, Map<String, Object> params) {
        JobDefinition jobDef = new JobDefinition(
                "TestJob", false, "TestJobRequest", "TestJobHandler",
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                false, null
        );
        return new ScheduledJobInfo(jobId, "Test Job", jobDef, Instant.now(), params, false);
    }

    private static ScheduledJobInfo externalJobInfo(UUID jobId) {
        JobDefinition jobDef = new JobDefinition(
                "TestJob", false, "TestJobRequest", "TestJobHandler",
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                true, "parameterSetId"
        );
        return new ScheduledJobInfo(jobId, "Test Job", jobDef, Instant.now(), Map.of(), false);
    }

    @Test
    @DisplayName("should return inline parameters unchanged")
    void execute_InlineParameters_ReturnsUnchanged() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Map<String, Object> inlineParams = Map.of("param1", "value1", "param2", 42);

        // Act
        Map<String, Object> result = useCase.execute(inlineJobInfo(jobId, inlineParams));

        // Assert
        assertThat(result)
                .isNotNull()
                .containsExactlyInAnyOrderEntriesOf(inlineParams);
        verify(parameterStorageService, never()).findById(any());
    }

    @Test
    @DisplayName("should resolve external parameters from storage using job ID as parameter set ID")
    void execute_ExternalParameters_ResolvesFromStorageByJobId() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Map<String, Object> storedParams = Map.of("externalParam1", "value1", "externalParam2", 100);
        ParameterSet parameterSet = ParameterSet.create(jobId, "TestJob", storedParams);

        when(parameterStorageService.findById(jobId)).thenReturn(Optional.of(parameterSet));

        // Act
        Map<String, Object> result = useCase.execute(externalJobInfo(jobId));

        // Assert
        assertThat(result)
                .isNotNull()
                .containsExactlyInAnyOrderEntriesOf(storedParams);
        verify(parameterStorageService).findById(jobId);
    }

    @Test
    @DisplayName("should return empty map when parameter set not found")
    void execute_ParameterSetNotFound_ReturnsEmptyMap() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        when(parameterStorageService.findById(jobId)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = useCase.execute(externalJobInfo(jobId));

        // Assert
        assertThat(result).isEmpty();
        verify(parameterStorageService).findById(jobId);
    }

    @Test
    @DisplayName("should return empty map for inline job with empty parameters")
    void execute_EmptyInlineParameters_ReturnsEmptyMap() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        // Act
        Map<String, Object> result = useCase.execute(inlineJobInfo(jobId, Map.of()));

        // Assert
        assertThat(result).isEmpty();
        verify(parameterStorageService, never()).findById(any());
    }
}
