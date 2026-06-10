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
            DELETE FROM jobrunr_control_batch_recap
            WHERE batch_job_id = ? AND child_job_id = ?
            """;

    private static final String INSERT_RECAP_SQL = """
            INSERT INTO jobrunr_control_batch_recap (batch_job_id, child_job_id, counter_name, counter_value)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_RECAP_ALL_COUNTERS_SQL = """
            SELECT counter_name, SUM(counter_value) AS total_counter_value
            FROM jobrunr_control_batch_recap
            WHERE batch_job_id = ?
            GROUP BY counter_name
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
                    recap.put(resultSet.getString("counter_name"), resultSet.getLong("total_counter_value"));
                }
            }
            return recap;
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to read recap for batchJobId %s", batchJobId);
            throw new IllegalStateException("Failed to read job recap", e);
        }
    }
}
