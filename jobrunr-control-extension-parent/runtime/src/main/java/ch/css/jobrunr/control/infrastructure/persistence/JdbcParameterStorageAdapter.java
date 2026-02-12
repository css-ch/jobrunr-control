package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.exceptions.ParameterSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-based implementation of ParameterStoragePort.
 * Stores parameters in a separate database table using pure SQL via Agroal DataSource.
 * Requires users to create the table manually (see DDL scripts in docs).
 */
@ApplicationScoped
public class JdbcParameterStorageAdapter implements ParameterStoragePort {

    private static final Logger LOG = Logger.getLogger(JdbcParameterStorageAdapter.class);

    private static final String INSERT_SQL = """
            INSERT INTO jobrunr_control_parameter_sets (id, job_type, parameters_json, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE jobrunr_control_parameter_sets
            SET job_type = ?, parameters_json = ?, updated_at = ?, version = version + 1
            WHERE id = ?
            """;

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, job_type, parameters_json, created_at, updated_at, version
            FROM jobrunr_control_parameter_sets
            WHERE id = ?
            """;

    private static final String DELETE_BY_ID_SQL = """
            DELETE FROM jobrunr_control_parameter_sets
            WHERE id = ?
            """;

    private final ObjectMapper objectMapper;
    private final AgroalDataSource dataSource;
    private final DatabaseTypeHandler databaseTypeHandler;

    @Inject
    public JdbcParameterStorageAdapter(
            ObjectMapper objectMapper,
            AgroalDataSource dataSource,
            DatabaseTypeHandler databaseTypeHandler) {
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
        this.databaseTypeHandler = databaseTypeHandler;
    }

    @Override
    public void store(ParameterSet parameterSet) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {

            String parametersJson = objectMapper.writeValueAsString(parameterSet.parameters());
            Instant now = Instant.now();

            stmt.setString(1, parameterSet.id().toString());
            stmt.setString(2, parameterSet.jobType());
            databaseTypeHandler.setJsonParameter(stmt, 3, parametersJson);
            stmt.setTimestamp(4, Timestamp.from(parameterSet.createdAt() != null ? parameterSet.createdAt() : now));
            stmt.setTimestamp(5, Timestamp.from(parameterSet.updatedAt() != null ? parameterSet.updatedAt() : now));
            stmt.setLong(6, 0L);

            stmt.executeUpdate();
            LOG.debugf("Stored parameter set: %s", parameterSet.id());

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to store parameter set: %s", parameterSet.id());
            throw new IllegalStateException("Failed to store parameter set", e);
        } catch (JsonProcessingException e) {
            throw new ParameterSerializationException("Failed to serialize parameters", e);
        }
    }

    @Override
    public void update(ParameterSet parameterSet) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            String parametersJson = objectMapper.writeValueAsString(parameterSet.parameters());
            Instant now = Instant.now();

            stmt.setString(1, parameterSet.jobType());
            databaseTypeHandler.setJsonParameter(stmt, 2, parametersJson);
            stmt.setTimestamp(3, Timestamp.from(now));
            stmt.setString(4, parameterSet.id().toString());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                // If no rows were updated, try to insert
                store(parameterSet);
            } else {
                LOG.debugf("Updated parameter set: %s", parameterSet.id());
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to update parameter set: %s", parameterSet.id());
            throw new IllegalStateException("Failed to update parameter set", e);
        } catch (JsonProcessingException e) {
            throw new ParameterSerializationException("Failed to serialize parameters", e);
        }
    }

    @Override
    public Optional<ParameterSet> findById(UUID id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setString(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String parametersJson = extractJsonFromResultSet(rs);
                    if (parametersJson == null) {
                        LOG.warnf("Parameter set %s has null JSON content", id);
                        return Optional.empty();
                    }

                    return deserializeParameterSet(id, rs, parametersJson);
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to find parameter set: %s", id);
            return Optional.empty();
        }
    }

    /**
     * Extracts JSON string from ResultSet using database-specific handler.
     */
    private String extractJsonFromResultSet(ResultSet rs) throws SQLException {
        return databaseTypeHandler.extractJson(rs);
    }

    /**
     * Deserializes a ParameterSet from ResultSet and JSON string.
     * Database-specific post-processing is handled by DatabaseTypeHandler.
     */
    private Optional<ParameterSet> deserializeParameterSet(UUID id, ResultSet rs, String parametersJson) {
        try {
            // Post-process JSON (handles H2 double-serialization, etc.)
            String jsonToDeserialize = databaseTypeHandler.processJsonAfterRead(id, parametersJson);

            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = objectMapper.readValue(jsonToDeserialize, Map.class);

            return Optional.of(new ParameterSet(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("job_type"),
                    parameters,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            ));
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to deserialize parameters for set: %s. JSON content (first 200 chars): %s",
                    id, parametersJson.length() > 200 ? parametersJson.substring(0, 200) + "..." : parametersJson);
            return Optional.empty();
        } catch (SQLException e) {
            LOG.errorf(e, "Failed to extract parameter set fields for: %s", id);
            return Optional.empty();
        }
    }

    @Override
    public void deleteById(UUID id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_BY_ID_SQL)) {

            stmt.setString(1, id.toString());
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                LOG.debugf("Deleted parameter set: %s", id);
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to delete parameter set: %s", id);
            throw new IllegalStateException("Failed to delete parameter set", e);
        }
    }
}

