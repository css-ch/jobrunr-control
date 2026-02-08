package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("InlineParameterStorageAdapter")
class InlineParameterStorageAdapterTest {

    private InlineParameterStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InlineParameterStorageAdapter();
    }

    @Test
    @DisplayName("store should do nothing (no-op implementation)")
    void store_AnyParameterSet_DoesNothing() {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", Map.of("key", "value"));

        // Act & Assert - should not throw any exception
        assertDoesNotThrow(() -> adapter.store(parameterSet));
        // This adapter stores parameters inline in JobRunr, so store() does nothing
    }

    @Test
    @DisplayName("findById should always return empty optional")
    void findById_AnyId_ReturnsEmpty() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        Optional<ParameterSet> result = adapter.findById(id);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteById should do nothing (no-op implementation)")
    void deleteById_AnyId_DoesNothing() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert - should not throw any exception
        assertDoesNotThrow(() -> adapter.deleteById(id));
    }

    @Test
    @DisplayName("updateLastAccessed should do nothing (no-op implementation)")
    void updateLastAccessed_AnyId_DoesNothing() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert - should not throw any exception
        assertDoesNotThrow(() -> adapter.updateLastAccessed(id));
    }

    @Test
    @DisplayName("should handle null UUID gracefully in findById")
    void findById_NullId_ReturnsEmpty() {
        // Act
        Optional<ParameterSet> result = adapter.findById(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("multiple operations should not affect each other")
    void multipleOperations_ShouldNotInterfere() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ParameterSet paramSet1 = ParameterSet.create(id1, "Job1", Map.of("a", "1"));
        ParameterSet paramSet2 = ParameterSet.create(id2, "Job2", Map.of("b", "2"));

        // Act & Assert - all operations should not throw
        assertDoesNotThrow(() -> adapter.store(paramSet1));
        assertDoesNotThrow(() -> adapter.store(paramSet2));

        Optional<ParameterSet> result1 = adapter.findById(id1);
        Optional<ParameterSet> result2 = adapter.findById(id2);

        assertDoesNotThrow(() -> adapter.deleteById(id1));
        assertDoesNotThrow(() -> adapter.updateLastAccessed(id2));

        // Assert - all findById should return empty for inline storage
        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
    }
}
