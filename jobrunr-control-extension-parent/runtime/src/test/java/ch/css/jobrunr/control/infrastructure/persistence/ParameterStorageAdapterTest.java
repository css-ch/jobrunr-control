package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParameterStorageAdapter")
class ParameterStorageAdapterTest {

    @Mock
    private Instance<ParameterStoragePort> storageAdapters;

    @Mock
    private Instance<JdbcParameterStorageAdapter> jdbcAdapterInstance;

    @Mock
    private JdbcParameterStorageAdapter jdbcAdapter;

    private ParameterStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ParameterStorageAdapter(storageAdapters);
    }

    @Test
    @DisplayName("should return true when external storage is available")
    void isExternalStorageAvailable_JdbcAdapterAvailable_ReturnsTrue() {
        // Arrange
        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(true);

        // Act
        boolean result = adapter.isExternalStorageAvailable();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when external storage is not available")
    void isExternalStorageAvailable_JpaAdapterNotAvailable_ReturnsFalse() {
        // Arrange
        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(false);

        // Act
        boolean result = adapter.isExternalStorageAvailable();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when checking storage availability throws exception")
    void isExternalStorageAvailable_ExceptionThrown_ReturnsFalse() {
        // Arrange
        when(storageAdapters.select(JdbcParameterStorageAdapter.class))
                .thenThrow(new RuntimeException("Storage check failed"));

        // Act
        boolean result = adapter.isExternalStorageAvailable();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should delegate store operation to JDBC adapter when available")
    void store_ExternalStorageAvailable_DelegatesToJpaAdapter() {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", Map.of("key", "value"));

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(true);
        when(jdbcAdapterInstance.get()).thenReturn(jdbcAdapter);

        // Act
        adapter.store(parameterSet);

        // Assert
        verify(jdbcAdapter).store(parameterSet);
    }

    @Test
    @DisplayName("should throw IllegalStateException when storing without available storage")
    void store_ExternalStorageNotAvailable_ThrowsException() {
        // Arrange
        ParameterSet parameterSet = ParameterSet.create(UUID.randomUUID(), "TestJob", Map.of());

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adapter.store(parameterSet))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("External parameter storage not available");
    }

    @Test
    @DisplayName("should delegate findById to JDBC adapter when available")
    void findById_ExternalStorageAvailable_DelegatesToJpaAdapter() {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSet expectedSet = ParameterSet.create(id, "TestJob", Map.of("key", "value"));

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(true);
        when(jdbcAdapterInstance.get()).thenReturn(jdbcAdapter);
        when(jdbcAdapter.findById(id)).thenReturn(Optional.of(expectedSet));

        // Act
        Optional<ParameterSet> result = adapter.findById(id);

        // Assert
        assertThat(result).isPresent().contains(expectedSet);
        verify(jdbcAdapter).findById(id);
    }

    @Test
    @DisplayName("should return empty when finding without available storage")
    void findById_ExternalStorageNotAvailable_ReturnsEmpty() {
        // Arrange
        UUID id = UUID.randomUUID();

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(false);

        // Act
        Optional<ParameterSet> result = adapter.findById(id);

        // Assert
        assertThat(result).isEmpty();
        verify(jdbcAdapter, never()).findById(any());
    }

    @Test
    @DisplayName("should delegate update operation to JDBC adapter when available")
    void update_ExternalStorageAvailable_DelegatesToJpaAdapter() {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", Map.of("key", "value"));

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(true);
        when(jdbcAdapterInstance.get()).thenReturn(jdbcAdapter);

        // Act
        adapter.update(parameterSet);

        // Assert
        verify(jdbcAdapter).update(parameterSet);
    }

    @Test
    @DisplayName("should throw IllegalStateException when updating without available storage")
    void update_ExternalStorageNotAvailable_ThrowsException() {
        // Arrange
        ParameterSet parameterSet = ParameterSet.create(UUID.randomUUID(), "TestJob", Map.of());

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adapter.update(parameterSet))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("External parameter storage not available");
    }

    @Test
    @DisplayName("should delegate deleteById to JDBC adapter when available")
    void deleteById_ExternalStorageAvailable_DelegatesToJpaAdapter() {
        // Arrange
        UUID id = UUID.randomUUID();

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(true);
        when(jdbcAdapterInstance.get()).thenReturn(jdbcAdapter);

        // Act
        adapter.deleteById(id);

        // Assert
        verify(jdbcAdapter).deleteById(id);
    }

    @Test
    @DisplayName("should do nothing when deleting without available storage")
    void deleteById_ExternalStorageNotAvailable_DoesNothing() {
        // Arrange
        UUID id = UUID.randomUUID();

        when(storageAdapters.select(JdbcParameterStorageAdapter.class)).thenReturn(jdbcAdapterInstance);
        when(jdbcAdapterInstance.isResolvable()).thenReturn(false);

        // Act
        adapter.deleteById(id);

        // Assert
        verify(jdbcAdapter, never()).deleteById(any());
    }
}
