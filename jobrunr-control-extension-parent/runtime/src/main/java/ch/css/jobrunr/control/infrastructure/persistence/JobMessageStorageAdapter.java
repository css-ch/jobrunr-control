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
            INSERT INTO "JOBRUNR_CONTROL_BATCH_MESSAGES" ("BATCH_JOB_ID", "CHILD_JOB_ID", "CREATED_AT", "LEVEL", "MESSAGE", "STACK_TRACE")
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SEARCH_SELECT_PREFIX = """
            SELECT "CREATED_AT", "CHILD_JOB_ID", "LEVEL", "MESSAGE", "STACK_TRACE"
            FROM "JOBRUNR_CONTROL_BATCH_MESSAGES"
            """;

    private static final String SEARCH_COUNT_PREFIX = """
            SELECT COUNT(*)
            FROM "JOBRUNR_CONTROL_BATCH_MESSAGES"
            """;

    private static final String COUNTERS_SQL = """
            SELECT "LEVEL", COUNT(*) AS level_count
            FROM "JOBRUNR_CONTROL_BATCH_MESSAGES"
            WHERE "BATCH_JOB_ID" = ?
            GROUP BY "LEVEL"
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
                ? " ORDER BY \"CREATED_AT\" DESC, \"ID\" DESC"
                : " ORDER BY \"CREATED_AT\" ASC, \"ID\" ASC";

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
                    String level = rs.getString("LEVEL");
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
                    Timestamp timestamp = rs.getTimestamp("CREATED_AT");
                    Instant createdAt = timestamp == null ? Instant.now() : timestamp.toInstant();
                    String childJobId = rs.getString("CHILD_JOB_ID");
                    messages.add(new JobMessage(
                            createdAt,
                            childJobId != null ? UUID.fromString(childJobId) : null,
                            JobMessageLevel.valueOf(rs.getString("LEVEL").toUpperCase(Locale.ROOT)),
                            rs.getString("MESSAGE"),
                            rs.getString("STACK_TRACE")
                    ));
                }
                return messages;
            }
        }
    }

    private SearchQueryParts buildSearchQueryParts(UUID jobId,
                                                   JobMessageLevelSearch levelSearch,
                                                   String normalizedTextSearch) {
        StringBuilder where = new StringBuilder(" WHERE \"BATCH_JOB_ID\" = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(jobId.toString());

        List<String> levelFilter = resolveLevelFilter(levelSearch);
        if (!levelFilter.isEmpty()) {
            where.append(" AND \"LEVEL\" IN (");
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
            where.append(" AND (LOWER(\"MESSAGE\") LIKE ? OR LOWER(\"STACK_TRACE\") LIKE ? OR LOWER(\"CHILD_JOB_ID\") LIKE ?)");
            String searchPattern = "%" + normalizedTextSearch + "%";
            parameters.add(searchPattern);
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
