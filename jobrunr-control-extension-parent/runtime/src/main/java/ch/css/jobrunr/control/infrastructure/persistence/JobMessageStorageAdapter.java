package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.details.JobMessage;
import ch.css.jobrunr.control.domain.details.JobMessageLevel;
import ch.css.jobrunr.control.domain.details.JobMessageLevelCounters;
import ch.css.jobrunr.control.domain.details.JobMessageLevelSearch;
import ch.css.jobrunr.control.domain.details.JobMessageSortOrder;
import ch.css.jobrunr.control.domain.details.JobMessagesPaged;
import io.agroal.api.AgroalDataSource;
import ch.css.jobrunr.control.domain.details.JobMessageStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class JobMessageStorageAdapter implements JobMessageStoragePort {

    private static final Logger LOG = Logger.getLogger(JobMessageStorageAdapter.class);

    private static final String INSERT_SQL = """
            INSERT INTO jobrunr_control_batch_messages (batch_job_id, child_job_id, created_at, level, message, stack_trace)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SEARCH_SELECT_PREFIX = """
            SELECT created_at, child_job_id, level, message, stack_trace
            FROM jobrunr_control_batch_messages
            """;

    private static final String SEARCH_COUNT_PREFIX = """
            SELECT COUNT(*)
            FROM jobrunr_control_batch_messages
            """;

    private static final String COUNTERS_SQL = """
            SELECT level, COUNT(*) AS level_count
            FROM jobrunr_control_batch_messages
            WHERE batch_job_id = ?
            GROUP BY level
            """;

    private final AgroalDataSource dataSource;
    private final DatabaseTypeHandler databaseTypeHandler;

    @Inject
    public JobMessageStorageAdapter(AgroalDataSource dataSource, DatabaseTypeHandler databaseTypeHandler) {
        this.dataSource = dataSource;
        this.databaseTypeHandler = databaseTypeHandler;
    }

    @Override
    public void writeMessage(UUID jobId, JobMessage message) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            stmt.setString(1, jobId.toString());
            stmt.setString(2, message.jobId() != null ? message.jobId().toString() : null);
            stmt.setTimestamp(3, Timestamp.from(message.createdAt() != null ? message.createdAt() : Instant.now()));
            stmt.setString(4, message.messageLevel().name());
            stmt.setString(5, message.message());
            stmt.setString(6, message.stackTrace());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to write job message for jobId %s", jobId);
            throw new IllegalStateException("Failed to write job message", e);
        }
    }

    @Override
    public JobMessagesPaged searchMessages(UUID jobId,
                                           JobMessageLevelSearch levelSearch,
                                           String textSearch,
                                           JobMessageSortOrder sortOrder,
                                           int pageNr,
                                           int pageSize) {
        int sanitizedPage = Math.max(0, pageNr);
        int sanitizedPageSize = pageSize <= 0 ? 10 : pageSize;
        int offset = sanitizedPage * sanitizedPageSize;
        JobMessageLevelSearch effectiveLevelSearch = levelSearch == null ? JobMessageLevelSearch.ALL : levelSearch;
        JobMessageSortOrder effectiveSortOrder = sortOrder == null ? JobMessageSortOrder.OLDEST_FIRST : sortOrder;
        String normalizedTextSearch = textSearch == null ? "" : textSearch.trim().toLowerCase(Locale.ROOT);

        SearchQueryParts queryParts = buildSearchQueryParts(jobId, effectiveLevelSearch, normalizedTextSearch);
        String orderBy = effectiveSortOrder == JobMessageSortOrder.NEWEST_FIRST
                ? " ORDER BY created_at DESC, id DESC"
                : " ORDER BY created_at ASC, id ASC";

        String pagedQuery = SEARCH_SELECT_PREFIX + queryParts.whereClause() + orderBy + paginationClause();
        String countQuery = SEARCH_COUNT_PREFIX + queryParts.whereClause();

        try (Connection conn = dataSource.getConnection()) {
            long totalMessages = readTotalMessages(conn, countQuery, queryParts.parameters());
            if (totalMessages == 0 || offset >= totalMessages) {
                return new JobMessagesPaged(List.of(), totalMessages, sanitizedPage, sanitizedPageSize);
            }

            List<JobMessage> messages = readMessages(
                    conn, pagedQuery, queryParts.parameters(), offset, sanitizedPageSize
            );
            return new JobMessagesPaged(messages, totalMessages, sanitizedPage, sanitizedPageSize);
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to search job messages for jobId %s", jobId);
            throw new IllegalStateException("Failed to search job messages", e);
        }
    }

    @Override
    public JobMessageLevelCounters determineMessageLevelCounters(UUID jobId) {
        long info = 0;
        long warning = 0;
        long error = 0;
        long exception = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNTERS_SQL)) {
            stmt.setString(1, jobId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String level = rs.getString("level");
                    long count = rs.getLong("level_count");
                    if (level == null) {
                        continue;
                    }

                    switch (JobMessageLevel.valueOf(level.toUpperCase(Locale.ROOT))) {
                        case INFO -> info = count;
                        case WARNING -> warning = count;
                        case ERROR -> error = count;
                        case EXCEPTION -> exception = count;
                    }
                }
            }
            return new JobMessageLevelCounters(info, warning, error, exception);
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to determine message counters for jobId %s", jobId);
            throw new IllegalStateException("Failed to determine message counters", e);
        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "Found unsupported message level while reading counters for jobId %s", jobId);
            throw new IllegalStateException("Failed to determine message counters", e);
        }
    }

    private long readTotalMessages(Connection conn, String sql, List<Object> parameters) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, parameters);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    private List<JobMessage> readMessages(Connection conn,
                                          String sql,
                                          List<Object> parameters,
                                          int offset,
                                          int pageSize) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = bindParameters(stmt, parameters);
            DatabaseTypeHandler.DatabaseType databaseType = databaseTypeHandler.getDatabaseType();
            if (databaseType == DatabaseTypeHandler.DatabaseType.ORACLE) {
                stmt.setInt(index, offset);
                stmt.setInt(index + 1, pageSize);
            } else {
                stmt.setInt(index, pageSize);
                stmt.setInt(index + 1, offset);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<JobMessage> messages = new ArrayList<>();
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    Instant createdAt = timestamp == null ? Instant.now() : timestamp.toInstant();
                    String childJobId = rs.getString("child_job_id");
                    messages.add(new JobMessage(
                            createdAt,
                            childJobId != null ? UUID.fromString(childJobId) : null,
                            JobMessageLevel.valueOf(rs.getString("level").toUpperCase(Locale.ROOT)),
                            rs.getString("message"),
                            rs.getString("stack_trace")
                    ));
                }
                return messages;
            }
        }
    }

    private SearchQueryParts buildSearchQueryParts(UUID jobId,
                                                   JobMessageLevelSearch levelSearch,
                                                   String normalizedTextSearch) {
        StringBuilder where = new StringBuilder(" WHERE batch_job_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(jobId.toString());

        List<String> levelFilter = resolveLevelFilter(levelSearch);
        if (!levelFilter.isEmpty()) {
            where.append(" AND level IN (");
            for (int i = 0; i < levelFilter.size(); i++) {
                if (i > 0) {
                    where.append(", ");
                }
                where.append("?");
            }
            where.append(")");
            parameters.addAll(levelFilter);
        }

        if (!normalizedTextSearch.isBlank()) {
            where.append(" AND (LOWER(message) LIKE ? OR LOWER(stack_trace) LIKE ?)");
            String searchPattern = "%" + normalizedTextSearch + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }
        return new SearchQueryParts(where.toString(), parameters);
    }

    private List<String> resolveLevelFilter(JobMessageLevelSearch levelSearch) {
        return switch (levelSearch) {
            case ALL -> List.of();
            case WARNINGS_AND_ERRORS_AND_EXCEPTIONS -> List.of(
                    JobMessageLevel.WARNING.name(),
                    JobMessageLevel.ERROR.name(),
                    JobMessageLevel.EXCEPTION.name()
            );
            case ERRORS_AND_EXCEPTIONS -> List.of(
                    JobMessageLevel.ERROR.name(),
                    JobMessageLevel.EXCEPTION.name()
            );
            case INFO_ONLY -> List.of(JobMessageLevel.INFO.name());
            case WARNING_ONLY -> List.of(JobMessageLevel.WARNING.name());
            case ERROR_ONLY -> List.of(JobMessageLevel.ERROR.name());
            case EXCEPTION_ONLY -> List.of(JobMessageLevel.EXCEPTION.name());
        };
    }

    private String paginationClause() {
        return databaseTypeHandler.getDatabaseType() == DatabaseTypeHandler.DatabaseType.ORACLE
                ? " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY"
                : " LIMIT ? OFFSET ?";
    }

    private int bindParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        int index = 1;
        for (Object parameter : parameters) {
            stmt.setObject(index++, parameter);
        }
        return index;
    }

    private record SearchQueryParts(String whereClause, List<Object> parameters) {
    }
}
