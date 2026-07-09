package ch.css.jobrunr.control.infrastructure.persistence;

import io.agroal.api.AgroalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobRecapStorageAdapter")
class JobRecapStorageAdapterTest {

    @Mock
    private AgroalDataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement deleteStatement;

    @Mock
    private PreparedStatement insertStatement;

    @Mock
    private PreparedStatement readStatement;

    @Mock
    private ResultSet resultSet;

    private JobRecapStorageAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new JobRecapStorageAdapter(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.getAutoCommit()).thenReturn(true);
    }

    @Test
    @DisplayName("should replace recap rows for a child job")
    void writeRecap_ValidCounters_ReplacesChildRows() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Map<String, Long> recap = Map.of("processed", 10L, "failed", 1L);

        when(connection.prepareStatement(anyString())).thenReturn(deleteStatement, insertStatement);
        when(deleteStatement.executeUpdate()).thenReturn(1);
        when(insertStatement.executeBatch()).thenReturn(new int[]{1, 1});

        adapter.writeRecap(batchId, childId, recap);

        verify(deleteStatement).setString(1, batchId.toString());
        verify(deleteStatement).setString(2, childId.toString());
        verify(deleteStatement).executeUpdate();
        verify(insertStatement, times(2)).addBatch();
        verify(insertStatement).executeBatch();
    }

    @Test
    @DisplayName("should only clear existing rows when recap map is empty")
    void writeRecap_EmptyRecap_OnlyDeletesChildRows() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(deleteStatement);
        when(deleteStatement.executeUpdate()).thenReturn(1);

        adapter.writeRecap(batchId, childId, Map.of());

        verify(deleteStatement).executeUpdate();
        verify(connection, never()).prepareStatement(contains("INSERT INTO \"JOBRUNR_CONTROL_BATCH_RECAP\""));
    }

    @Test
    @DisplayName("should aggregate requested counters by name for a batch")
    void readRecap_FilteredCounters_ReturnsAggregatedValues() throws Exception {
        UUID batchId = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(readStatement);
        when(readStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("COUNTER_NAME")).thenReturn("processed", "failed");
        when(resultSet.getLong("total_counter_value")).thenReturn(14L, 2L);

        Map<String, Long> recap = adapter.readRecap(batchId);

        assertThat(recap).containsEntry("processed", 14L);
        assertThat(recap).containsEntry("failed", 2L);
        verify(readStatement).setString(1, batchId.toString());
    }

    @Test
    @DisplayName("should return all available counters when no filter is provided")
    void readRecap_NoFilter_ReturnsAllCounters() throws Exception {
        UUID batchId = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(readStatement);
        when(readStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("COUNTER_NAME")).thenReturn("processed");
        when(resultSet.getLong("total_counter_value")).thenReturn(5L);

        Map<String, Long> recap = adapter.readRecap(batchId);

        assertThat(recap).containsExactlyInAnyOrderEntriesOf(Map.of("processed", 5L));
        verify(readStatement).setString(1, batchId.toString());
    }

    @Test
    @DisplayName("should throw IllegalStateException when write fails")
    void writeRecap_SqlException_ThrowsIllegalStateException() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        assertThatThrownBy(() -> adapter.writeRecap(batchId, childId, Map.of("processed", 1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to write job recap");
    }
}
