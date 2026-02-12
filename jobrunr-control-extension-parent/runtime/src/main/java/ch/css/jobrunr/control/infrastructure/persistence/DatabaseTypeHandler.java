package ch.css.jobrunr.control.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.*;
import java.util.UUID;

/**
 * Handles database-specific behaviors for parameter storage.
 * <p>
 * Different databases have different ways of handling JSON columns:
 * - PostgreSQL: Uses JSONB type, requires Types.OTHER for binding
 * - H2: Uses CLOB, may double-serialize JSON strings
 * - Oracle: Uses JSON type (native since 21c), requires getString() to avoid ORA-18722
 * - MySQL: Uses JSON type
 *
 * @see <a href="https://docs.oracle.com/error-help/db/ora-18722/">Oracle ORA-18722 Error</a>
 */
@ApplicationScoped
public class DatabaseTypeHandler {

    private static final Logger LOG = Logger.getLogger(DatabaseTypeHandler.class);

    private final ObjectMapper objectMapper;
    private final AgroalDataSource dataSource;

    private DatabaseType databaseType;

    @Inject
    public DatabaseTypeHandler(ObjectMapper objectMapper, AgroalDataSource dataSource) {
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
    }

    @PostConstruct
    void detectDatabaseType() {
        try (Connection conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName();
            databaseType = detectTypeFromProductName(productName);
            LOG.infof("Detected database type: %s (product: %s)", databaseType, productName);
        } catch (SQLException e) {
            LOG.warnf(e, "Could not detect database type, defaulting to GENERIC");
            databaseType = DatabaseType.GENERIC;
        }
    }

    private DatabaseType detectTypeFromProductName(String productName) {
        if (productName == null) {
            return DatabaseType.GENERIC;
        }

        String lower = productName.toLowerCase();
        if (lower.contains("postgresql")) {
            return DatabaseType.POSTGRESQL;
        } else if (lower.contains("h2")) {
            return DatabaseType.H2;
        } else if (lower.contains("oracle")) {
            return DatabaseType.ORACLE;
        } else if (lower.contains("mysql")) {
            return DatabaseType.MYSQL;
        }

        return DatabaseType.GENERIC;
    }

    /**
     * Sets JSON parameter in PreparedStatement using database-specific binding.
     *
     * @param stmt  the PreparedStatement
     * @param index the parameter index
     * @param json  the JSON string to bind
     * @throws SQLException if binding fails
     */
    public void setJsonParameter(PreparedStatement stmt, int index, String json) throws SQLException {
        switch (databaseType) {
            case POSTGRESQL -> stmt.setObject(index, json, Types.OTHER);
            default -> stmt.setString(index, json);
        }
    }

    /**
     * Extracts JSON string from ResultSet, handling database-specific behaviors.
     *
     * @param rs the ResultSet
     * @return the JSON string
     * @throws SQLException if extraction fails
     */
    public String extractJson(ResultSet rs) throws SQLException {
        // Oracle requires getString() to avoid ORA-18722 error
        // (Connection property oracle.jdbc.jsonDefaultGetObjectType is not set)
        if (databaseType == DatabaseType.ORACLE) {
            return rs.getString("parameters_json");
        }

        Object jsonObject = rs.getObject("parameters_json");

        // Some databases (H2, MySQL) return String directly
        if (jsonObject instanceof String) {
            return (String) jsonObject;
        }

        // PostgreSQL and others return via getString()
        return rs.getString("parameters_json");
    }

    /**
     * Post-processes JSON string after reading from database.
     * Handles database-specific quirks like H2's double-serialization.
     *
     * @param id             the parameter set ID (for logging)
     * @param parametersJson the raw JSON string from database
     * @return processed JSON string ready for deserialization
     */
    public String processJsonAfterRead(UUID id, String parametersJson) {
        return switch (databaseType) {
            case H2 -> unescapeH2DoubleSerializedJson(id, parametersJson);
            default -> parametersJson;
        };
    }

    /**
     * Handles H2 CLOB double-serialization issue.
     * <p>
     * H2's CLOB type may store JSON as a JSON-escaped string: "{"key":"value"}"
     * This method detects and unescapes such strings automatically.
     *
     * @param id             the parameter set ID (for logging)
     * @param parametersJson the JSON string from the database
     * @return unescaped JSON string ready for deserialization
     */
    private String unescapeH2DoubleSerializedJson(UUID id, String parametersJson) {
        // Detect double-serialization: JSON wrapped in quotes
        if (parametersJson.startsWith("\"") && parametersJson.endsWith("\"")) {
            try {
                String unescaped = objectMapper.readValue(parametersJson, String.class);
                LOG.debugf("Detected H2 double-serialized JSON for parameter set %s, unescaped successfully", id);
                return unescaped;
            } catch (JsonProcessingException e) {
                LOG.debugf("Could not unescape JSON string for parameter set %s, using as-is", id);
                // Fallback to original if unescaping fails
            }
        }
        return parametersJson;
    }

    /**
     * Returns the detected database type.
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * Enum representing supported database types.
     */
    public enum DatabaseType {
        POSTGRESQL,
        H2,
        ORACLE,
        MYSQL,
        GENERIC
    }
}

