package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.details.JobMessage;
import ch.css.jobrunr.control.domain.details.JobMessageLevel;
import ch.css.jobrunr.control.domain.details.JobMessageLevelCounters;
import ch.css.jobrunr.control.domain.details.JobMessageLevelSearch;
import ch.css.jobrunr.control.domain.details.JobMessageSortOrder;
import ch.css.jobrunr.control.domain.details.JobMessagesPaged;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobMessageStorageAdapter")
class JobMessageStorageAdapterTest {

    @Mock
    private AgroalDataSource dataSource;

    @Mock
    private DatabaseTypeHandler databaseTypeHandler;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement insertStatement;

    @Mock
    private PreparedStatement countStatement;

    @Mock
    private PreparedStatement searchStatement;

    @Mock
    private PreparedStatement countersStatement;

    @Mock
    private ResultSet countResultSet;

    @Mock
    private ResultSet searchResultSet;

    @Mock
    private ResultSet countersResultSet;

    private JobMessageStorageAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new JobMessageStorageAdapter(dataSource, databaseTypeHandler);
        when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(databaseTypeHandler.getDatabaseType()).thenReturn(DatabaseTypeHandler.DatabaseType.H2);
    }

    @Test
    @DisplayName("should persist a message entry")
    void writeMessage_ValidMessage_PersistsEntry() throws Exception {
        // Given
        UUID batchId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        JobMessage message = new JobMessage(Instant.now(), childId, JobMessageLevel.WARNING, "Warning text", "stack");

        when(connection.prepareStatement(anyString())).thenReturn(insertStatement);
        when(insertStatement.executeUpdate()).thenReturn(1);

        // When
        adapter.writeMessage(batchId, message);

        // Then
        verify(insertStatement).setString(1, batchId.toString());
        verify(insertStatement).setString(4, JobMessageLevel.WARNING.name());
        verify(insertStatement).setString(5, "Warning text");
        verify(insertStatement).setString(6, "stack");
        verify(insertStatement).executeUpdate();
    }

    @Test
    @DisplayName("should return paged messages from storage")
    void searchMessages_ExistingMessages_ReturnsPagedResult() throws Exception {
        // Given
        UUID batchId = UUID.randomUUID();
        Instant now = Instant.now();

        when(connection.prepareStatement(anyString())).thenReturn(countStatement, searchStatement);
        when(countStatement.executeQuery()).thenReturn(countResultSet);
        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getLong(1)).thenReturn(1L);

        when(searchStatement.executeQuery()).thenReturn(searchResultSet);
        when(searchResultSet.next()).thenReturn(true, false);
        when(searchResultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(now));
        when(searchResultSet.getString("child_job_id")).thenReturn(UUID.randomUUID().toString());
        when(searchResultSet.getString("level")).thenReturn(JobMessageLevel.ERROR.name());
        when(searchResultSet.getString("message")).thenReturn("failed");
        when(searchResultSet.getString("stack_trace")).thenReturn("trace");

        // When
        JobMessagesPaged result = adapter.searchMessages(
                batchId,
                JobMessageLevelSearch.ALL,
                "",
                JobMessageSortOrder.NEWEST_FIRST,
                0,
                10
        );

        // Then
        assertThat(result.totalMessages()).isEqualTo(1L);
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().getFirst().messageLevel()).isEqualTo(JobMessageLevel.ERROR);
        verify(searchStatement).setInt(2, 10);
        verify(searchStatement).setInt(3, 0);
    }

    @Test
    @DisplayName("should aggregate counters grouped by level")
    void determineMessageLevelCounters_LevelsPresent_ReturnsCounters() throws Exception {
        // Given
        UUID batchId = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(countersStatement);
        when(countersStatement.executeQuery()).thenReturn(countersResultSet);
        when(countersResultSet.next()).thenReturn(true, true, true, false);
        when(countersResultSet.getString("level")).thenReturn(
                JobMessageLevel.INFO.name(),
                JobMessageLevel.WARNING.name(),
                JobMessageLevel.EXCEPTION.name()
        );
        when(countersResultSet.getLong("level_count")).thenReturn(4L, 2L, 1L);

        // When
        JobMessageLevelCounters counters = adapter.determineMessageLevelCounters(batchId);

        // Then
        assertThat(counters.infoMessages()).isEqualTo(4L);
        assertThat(counters.warningMessages()).isEqualTo(2L);
        assertThat(counters.errorMessages()).isZero();
        assertThat(counters.exceptionMessages()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should throw IllegalStateException when write fails")
    void writeMessage_SqlException_ThrowsIllegalStateException() throws Exception {
        // Given
        UUID batchId = UUID.randomUUID();
        JobMessage message = new JobMessage(Instant.now(), UUID.randomUUID(), JobMessageLevel.INFO, "ok", null);
        when(connection.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("DB error"));

        // When / Then
        assertThatThrownBy(() -> adapter.writeMessage(batchId, message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to write job message");
    }
}

