package ch.css.jobrunr.control.application.parameters;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
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

    @Test
    @DisplayName("should return inline parameters unchanged")
    void execute_InlineParameters_ReturnsUnchanged() {
        // Arrange
        Map<String, Object> inlineParams = Map.of(
                "param1", "value1",
                "param2", 42
        );

        // Act
        Map<String, Object> result = useCase.execute(inlineParams);

        // Assert
        assertThat(result)
                .isNotNull()
                .containsExactlyInAnyOrderEntriesOf(inlineParams);
        verify(parameterStorageService, never()).findById(any());
    }

    @Test
    @DisplayName("should resolve external parameters from storage")
    void execute_ExternalParameters_ResolvesFromStorage() {
        // Arrange
        UUID paramSetId = UUID.randomUUID();
        Map<String, Object> externalParamsMap = Map.of("__parameterSetId", paramSetId.toString());
        Map<String, Object> storedParams = Map.of(
                "externalParam1", "value1",
                "externalParam2", 100
        );
        ParameterSet parameterSet = ParameterSet.create(paramSetId, "TestJob", storedParams);

        when(parameterStorageService.findById(paramSetId)).thenReturn(Optional.of(parameterSet));

        // Act
        Map<String, Object> result = useCase.execute(externalParamsMap);

        // Assert
        assertThat(result)
                .isNotNull()
                .containsExactlyInAnyOrderEntriesOf(storedParams);
        verify(parameterStorageService).findById(paramSetId);
    }

    @Test
    @DisplayName("should return empty map when parameter set not found")
    void execute_ParameterSetNotFound_ReturnsEmptyMap() {
        // Arrange
        UUID paramSetId = UUID.randomUUID();
        Map<String, Object> externalParamsMap = Map.of("__parameterSetId", paramSetId.toString());

        when(parameterStorageService.findById(paramSetId)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = useCase.execute(externalParamsMap);

        // Assert
        assertThat(result).isEmpty();
        verify(parameterStorageService).findById(paramSetId);
    }

    @Test
    @DisplayName("should return empty map for null parameters")
    void execute_NullParameters_ReturnsEmptyMap() {
        // Act
        Map<String, Object> result = useCase.execute(null);

        // Assert
        assertThat(result).isEmpty();
        verify(parameterStorageService, never()).findById(any());
    }

    @Test
    @DisplayName("should return empty map for empty parameters")
    void execute_EmptyParameters_ReturnsEmptyMap() {
        // Act
        Map<String, Object> result = useCase.execute(new HashMap<>());

        // Assert
        assertThat(result).isEmpty();
        verify(parameterStorageService, never()).findById(any());
    }

    @Test
    @DisplayName("should detect inline parameters do not use external storage")
    void usesExternalStorage_InlineParameters_ReturnsFalse() {
        // Arrange
        Map<String, Object> inlineParams = Map.of("param1", "value1");

        // Act
        boolean result = useCase.usesExternalStorage(inlineParams);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should detect external parameters use external storage")
    void usesExternalStorage_ExternalParameters_ReturnsTrue() {
        // Arrange
        Map<String, Object> externalParams = Map.of("__parameterSetId", UUID.randomUUID().toString());

        // Act
        boolean result = useCase.usesExternalStorage(externalParams);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should handle invalid parameter set ID format")
    void execute_InvalidParameterSetId_ReturnsEmptyMap() {
        // Arrange
        Map<String, Object> invalidParams = Map.of("__parameterSetId", "not-a-uuid");

        // Act
        Map<String, Object> result = useCase.execute(invalidParams);

        // Assert
        assertThat(result).isEmpty();
        verify(parameterStorageService, never()).findById(any());
    }
}
