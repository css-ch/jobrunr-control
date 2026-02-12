package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JdbcParameterStorageAdapter.
 * Tests JDBC-based parameter storage functionality using mocks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JDBC Parameter Storage Adapter")
class JdbcParameterStorageAdapterTest {

    @Mock
    private AgroalDataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private DatabaseTypeHandler databaseTypeHandler;

    private ObjectMapper objectMapper;
    private JdbcParameterStorageAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        adapter = new JdbcParameterStorageAdapter(objectMapper, dataSource, databaseTypeHandler);

        // Default mock behavior
        when(dataSource.getConnection()).thenReturn(connection);

        // Default database type handler behavior (pass-through for most operations)
        // Use lenient() to avoid unnecessary stubbing warnings
        lenient().doAnswer(invocation -> {
            PreparedStatement stmt = invocation.getArgument(0);
            int index = invocation.getArgument(1);
            String json = invocation.getArgument(2);
            stmt.setString(index, json);
            return null;
        }).when(databaseTypeHandler).setJsonParameter(any(), anyInt(), anyString());

        lenient().when(databaseTypeHandler.extractJson(any())).thenAnswer(invocation -> {
            ResultSet rs = invocation.getArgument(0);
            return rs.getString("parameters_json");
        });

        lenient().when(databaseTypeHandler.processJsonAfterRead(any(), anyString())).thenAnswer(invocation -> {
            return invocation.getArgument(1); // Return JSON as-is
        });
    }

    @Test
    @DisplayName("should store parameter set successfully")
    void store_ValidParameterSet_StoresSuccessfully() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key", "value");
        ParameterSet parameterSet = new ParameterSet(id, "TestJob", params, Instant.now(), Instant.now());

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        adapter.store(parameterSet);

        // Then
        verify(preparedStatement).setString(1, id.toString());
        verify(preparedStatement).setString(2, "TestJob");
        verify(preparedStatement).setString(eq(3), anyString()); // JSON string
        verify(preparedStatement).executeUpdate();
        verify(connection).close();
    }

    @Test
    @DisplayName("should find parameter set by ID")
    void findById_ExistingId_ReturnsParameterSet() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        String jsonParams = "{\"key\":\"value\"}";
        Instant now = Instant.now();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn(id.toString());
        when(resultSet.getString("job_type")).thenReturn("TestJob");
        when(resultSet.getString("parameters_json")).thenReturn(jsonParams);
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(now));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(now));

        // When
        Optional<ParameterSet> result = adapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().jobType()).isEqualTo("TestJob");
        assertThat(result.get().parameters()).containsEntry("key", "value");
        verify(preparedStatement).setString(1, id.toString());
        verify(connection).close();
    }

    @Test
    @DisplayName("should return empty when parameter set not found")
    void findById_NonExistingId_ReturnsEmpty() throws Exception {
        // Given
        UUID id = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Optional<ParameterSet> result = adapter.findById(id);

        // Then
        assertThat(result).isEmpty();
        verify(connection).close();
    }

    @Test
    @DisplayName("should update existing parameter set")
    void update_ExistingParameterSet_UpdatesSuccessfully() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key", "updatedValue");
        ParameterSet parameterSet = new ParameterSet(id, "TestJob", params, Instant.now(), Instant.now());

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        adapter.update(parameterSet);

        // Then
        verify(preparedStatement).setString(1, "TestJob");
        verify(preparedStatement).setString(eq(2), anyString()); // JSON string
        verify(preparedStatement).setString(4, id.toString());
        verify(preparedStatement).executeUpdate();
        verify(connection).close();
    }

    @Test
    @DisplayName("should delete parameter set by ID")
    void deleteById_ExistingId_DeletesSuccessfully() throws Exception {
        // Given
        UUID id = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        adapter.deleteById(id);

        // Then
        verify(preparedStatement).setString(1, id.toString());
        verify(preparedStatement).executeUpdate();
        verify(connection).close();
    }

    @Test
    @DisplayName("should handle SQL exception when storing")
    void store_SqlException_ThrowsIllegalStateException() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key", "value");
        ParameterSet parameterSet = new ParameterSet(id, "TestJob", params, Instant.now(), Instant.now());

        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Connection error"));

        // When/Then
        assertThatThrownBy(() -> adapter.store(parameterSet))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to store parameter set");
    }

    @Test
    @DisplayName("should return empty when deserialization fails")
    void findById_InvalidJson_ReturnsEmpty() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        String invalidJson = "{invalid json}";  // This mimics old JPA format

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        // Only stub parameters_json since the JSON parsing fails before reading other columns
        when(resultSet.getString("parameters_json")).thenReturn(invalidJson);

        // When
        Optional<ParameterSet> result = adapter.findById(id);

        // Then
        assertThat(result).isEmpty();
        verify(connection).close();
        // Note: Error log will include the invalid JSON content for debugging
    }
}

