package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.exceptions.ParameterSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaParameterStorageAdapter")
class JpaParameterStorageAdapterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Instance<EntityManager> entityManagerInstance;

    @Mock
    private EntityManager entityManager;

    private JpaParameterStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(entityManagerInstance.isResolvable()).thenReturn(true);
        lenient().when(entityManagerInstance.get()).thenReturn(entityManager);
        adapter = new JpaParameterStorageAdapter(objectMapper, entityManagerInstance);
    }

    @Test
    @DisplayName("should store parameter set successfully")
    void store_ValidParameterSet_PersistsEntity() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key1", "value1", "key2", 42);
        Instant now = Instant.now();
        ParameterSet parameterSet = new ParameterSet(id, "TestJob", params, now, now);
        String jsonString = "{\"key1\":\"value1\",\"key2\":42}";

        when(objectMapper.writeValueAsString(params)).thenReturn(jsonString);

        // Act
        adapter.store(parameterSet);

        // Assert
        ArgumentCaptor<ParameterSetEntity> entityCaptor = ArgumentCaptor.forClass(ParameterSetEntity.class);
        verify(entityManager).persist(entityCaptor.capture());

        ParameterSetEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.id).isEqualTo(id);
        assertThat(capturedEntity.jobType).isEqualTo("TestJob");
        assertThat(capturedEntity.parametersJson).isEqualTo(jsonString);
    }

    @Test
    @DisplayName("should throw ParameterSerializationException when JSON serialization fails")
    void store_SerializationFails_ThrowsException() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key", "value");
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", params);

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {
        });

        // Act & Assert
        assertThatThrownBy(() -> adapter.store(parameterSet))
                .isInstanceOf(ParameterSerializationException.class)
                .hasMessageContaining("Failed to serialize parameters");
    }

    @Test
    @DisplayName("should find parameter set by ID")
    void findById_ExistingId_ReturnsParameterSet() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ParameterSetEntity entity = new ParameterSetEntity();
        entity.id = id;
        entity.jobType = "TestJob";
        entity.parametersJson = "{\"key\":\"value\"}";
        entity.createdAt = now;
        entity.updatedAt = now;

        Map<String, Object> expectedParams = Map.of("key", "value");

        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(entity);
        when(objectMapper.readValue(entity.parametersJson, Map.class)).thenReturn(expectedParams);

        // Act
        Optional<ParameterSet> result = adapter.findById(id);

        // Assert
        assertThat(result).isPresent();
        ParameterSet parameterSet = result.get();
        assertThat(parameterSet.id()).isEqualTo(id);
        assertThat(parameterSet.jobType()).isEqualTo("TestJob");
        assertThat(parameterSet.parameters()).containsEntry("key", "value");
        assertThat(parameterSet.createdAt()).isEqualTo(now);
        assertThat(parameterSet.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should return empty optional when parameter set not found")
    void findById_NonExistingId_ReturnsEmpty() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(null);

        // Act
        Optional<ParameterSet> result = adapter.findById(id);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty optional when deserialization fails")
    void findById_DeserializationFails_ReturnsEmpty() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSetEntity entity = new ParameterSetEntity();
        entity.id = id;
        entity.jobType = "TestJob";
        entity.parametersJson = "invalid-json";
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();

        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(entity);
        when(objectMapper.readValue(eq("invalid-json"), eq(Map.class)))
                .thenThrow(new JsonProcessingException("Deserialization error") {
                });

        // Act
        Optional<ParameterSet> result = adapter.findById(id);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delete parameter set by ID")
    void deleteById_ExistingId_RemovesEntity() {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSetEntity entity = new ParameterSetEntity();
        entity.id = id;

        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(entity);

        // Act
        adapter.deleteById(id);

        // Assert
        verify(entityManager).remove(entity);
    }

    @Test
    @DisplayName("should do nothing when deleting non-existing parameter set")
    void deleteById_NonExistingId_DoesNothing() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(null);

        // Act
        adapter.deleteById(id);

        // Assert
        verify(entityManager, never()).remove(any());
    }

    @Test
    @DisplayName("should update existing parameter set in-place")
    void update_ExistingParameterSet_UpdatesEntity() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        Map<String, Object> newParams = Map.of("key1", "updated");
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", newParams);
        String newJson = "{\"key1\":\"updated\"}";

        ParameterSetEntity existingEntity = new ParameterSetEntity();
        existingEntity.id = id;
        existingEntity.jobType = "TestJob";
        existingEntity.parametersJson = "{\"key1\":\"old\"}";

        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(existingEntity);
        when(objectMapper.writeValueAsString(newParams)).thenReturn(newJson);

        // Act
        adapter.update(parameterSet);

        // Assert - entity fields updated in-place, no persist called
        assertThat(existingEntity.parametersJson).isEqualTo(newJson);
        assertThat(existingEntity.jobType).isEqualTo("TestJob");
        verify(entityManager, never()).persist(any());
    }

    @Test
    @DisplayName("should fall back to store when updating non-existing parameter set")
    void update_NonExistingParameterSet_FallsBackToStore() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key1", "value1");
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", params);
        String jsonString = "{\"key1\":\"value1\"}";

        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(null);
        when(objectMapper.writeValueAsString(params)).thenReturn(jsonString);

        // Act
        adapter.update(parameterSet);

        // Assert - falls back to persist
        verify(entityManager).persist(any(ParameterSetEntity.class));
    }

    @Test
    @DisplayName("should throw ParameterSerializationException when update serialization fails")
    void update_SerializationFails_ThrowsException() throws JsonProcessingException {
        // Arrange
        UUID id = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(id, "TestJob", Map.of("key", "value"));

        ParameterSetEntity existingEntity = new ParameterSetEntity();
        existingEntity.id = id;

        when(entityManager.find(ParameterSetEntity.class, id)).thenReturn(existingEntity);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        // Act & Assert
        assertThatThrownBy(() -> adapter.update(parameterSet))
                .isInstanceOf(ParameterSerializationException.class)
                .hasMessageContaining("Failed to serialize parameters");
    }

    @Test
    @DisplayName("should throw IllegalStateException when EntityManager not available")
    void store_EntityManagerNotAvailable_ThrowsException() {
        // Arrange
        when(entityManagerInstance.isResolvable()).thenReturn(false);
        JpaParameterStorageAdapter adapterWithoutEM = new JpaParameterStorageAdapter(objectMapper, entityManagerInstance);
        ParameterSet parameterSet = ParameterSet.create(UUID.randomUUID(), "TestJob", Map.of());

        // Act & Assert
        assertThatThrownBy(() -> adapterWithoutEM.store(parameterSet))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EntityManager is not available");
    }
}
