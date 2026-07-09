package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.details.JobRecapStoragePort;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JobRecapStorageAdapter implements JobRecapStoragePort {

    private static final Logger LOG = Logger.getLogger(JobRecapStorageAdapter.class);

    private static final String DELETE_RECAP_FOR_CHILD_SQL = """
            DELETE FROM "JOBRUNR_CONTROL_BATCH_RECAP"
            WHERE "BATCH_JOB_ID" = ? AND "CHILD_JOB_ID" = ?
            """;

    private static final String INSERT_RECAP_SQL = """
            INSERT INTO "JOBRUNR_CONTROL_BATCH_RECAP" ("BATCH_JOB_ID", "CHILD_JOB_ID", "COUNTER_NAME", "COUNTER_VALUE")
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_RECAP_ALL_COUNTERS_SQL = """
            SELECT "COUNTER_NAME", SUM("COUNTER_VALUE") AS total_counter_value
            FROM "JOBRUNR_CONTROL_BATCH_RECAP"
            WHERE "BATCH_JOB_ID" = ?
            GROUP BY "COUNTER_NAME"
            """;

    private final AgroalDataSource dataSource;

    @Inject
    public JobRecapStorageAdapter(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void writeRecap(UUID batchJobId, UUID childJobId, Map<String, Long> recap) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement deleteStatement = conn.prepareStatement(DELETE_RECAP_FOR_CHILD_SQL)) {
                deleteStatement.setString(1, batchJobId.toString());
                deleteStatement.setString(2, childJobId.toString());
                deleteStatement.executeUpdate();
            }

            if (recap != null) {
                recap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() == 0L);
            }

            if (recap != null && !recap.isEmpty()) {
                try (PreparedStatement insertStatement = conn.prepareStatement(INSERT_RECAP_SQL)) {
                    for (Map.Entry<String, Long> recapEntry : recap.entrySet()) {
                        insertStatement.setString(1, batchJobId.toString());
                        insertStatement.setString(2, childJobId.toString());
                        insertStatement.setString(3, recapEntry.getKey());
                        insertStatement.setLong(4, recapEntry.getValue() == null ? 0L : recapEntry.getValue());
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
            }
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to write recap for batchJobId %s and childJobId %s", batchJobId, childJobId);
            throw new IllegalStateException("Failed to write job recap", e);
        }
    }

    @Override
    public Map<String, Long> readRecap(UUID batchJobId) {
        Map<String, Long> recap = new HashMap<>();

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(SELECT_RECAP_ALL_COUNTERS_SQL)) {
            stmt.setString(1, batchJobId.toString());

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    recap.put(resultSet.getString("COUNTER_NAME"), resultSet.getLong("total_counter_value"));
                }
            }
            return recap;
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to read recap for batchJobId %s", batchJobId);
            throw new IllegalStateException("Failed to read job recap", e);
        }
    }
}
