package ca.jolt.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;

public abstract class Broker<T> {
    private static final Logger logger = Logger.getLogger(Broker.class.getName());
    protected final String table;
    protected final Class<T> entityClass;
    private final ObjectMapper objectMapper;

    protected Broker(String table, Class<T> entityClass) {
        if (!SqlSecurity.isValidIdentifier(table)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The identifier for the class is not valid ! Please change it.",
                    "The table name was found in our reserved keywords SQL-list. If you believe this to be an error, please report it as fast as possible to our github repository.",
                    null);
        }
        this.table = table;
        this.entityClass = entityClass;
        this.objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected Optional<T> selectOne(String sql, Object... params) {
        if (!SqlSecurity.isValidRawSql(sql)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The SQL query is not valid ! Please change it.",
                    "The SQL query was found to be invalid. If you believe this to be an error, please report it as fast as possible to our github repository.",
                    null);
        }

        Connection connection = null;
        try {
            connection = Database.getInstance().getConnection();
            connection.setAutoCommit(true);

            try (PreparedStatement stmt = prepareStatement(connection, sql, params);
                    ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(mapToEntity(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.severe(() -> "Database query error: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred. Please try again later.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        } finally {
            Database.getInstance().releaseConnection(connection);
        }
    }

    protected List<T> selectMany(String sql, Object... params) {
        if (!SqlSecurity.isValidRawSql(sql)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The SQL query is not valid ! Please change it.",
                    "The SQL query was found to be invalid. If you believe this to be an error, please report it as fast as possible to our github repository.",
                    null);
        }

        Connection connection = null;
        try {
            connection = Database.getInstance().getConnection();
            connection.setAutoCommit(true);

            try (PreparedStatement stmt = prepareStatement(connection, sql, params);
                    ResultSet rs = stmt.executeQuery()) {

                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapToEntity(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            logger.severe(() -> "Database query error: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred. Please try again later.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        } finally {
            Database.getInstance().releaseConnection(connection);
        }
    }

    protected boolean query(String sql, Object... params) {
        if (!SqlSecurity.isValidRawSql(sql)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The SQL query is not valid ! Please change it. The SQL query was found to be invalid. If you believe this to be an error",
                    "please report it as fast as possible to our github repository.",
                    null);
        }

        Connection connection = null;
        try {
            connection = Database.getInstance().getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement stmt = prepareStatement(connection, sql, params)) {
                int rowsAffected = stmt.executeUpdate();
                connection.commit();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            rollbackSilently(connection);
            logger.severe(() -> "Database execution error: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred. Please try again later.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        } finally {
            resetConnectionAndRelease(connection);
        }
    }

    protected T mapToEntity(ResultSet rs) throws SQLException {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();

            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                Object value = rs.getObject(i);

                if (value == null) {
                    jsonNode.putNull(columnName);
                } else if (value instanceof String) {
                    jsonNode.put(columnName, (String) value);
                } else if (value instanceof Integer) {
                    jsonNode.put(columnName, (Integer) value);
                } else if (value instanceof Long) {
                    jsonNode.put(columnName, (Long) value);
                } else if (value instanceof Double) {
                    jsonNode.put(columnName, (Double) value);
                } else if (value instanceof Boolean) {
                    jsonNode.put(columnName, (Boolean) value);
                } else if (value instanceof java.sql.Date) {
                    jsonNode.put(columnName, value.toString());
                } else if (value instanceof java.sql.Timestamp) {
                    jsonNode.put(columnName, value.toString());
                } else {
                    jsonNode.put(columnName, value.toString());
                }
            }

            return objectMapper.treeToValue(jsonNode, entityClass);
        } catch (Exception e) {
            logger.severe(() -> "Error mapping ResultSet to entity: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred. Please try again later.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        }
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }

    private void rollbackSilently(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            logger.warning(() -> "Failed to rollback transaction: " + e.getMessage());
        }
    }

    private void resetConnectionAndRelease(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.warning(() -> "Failed to reset connection state: " + e.getMessage());
        } finally {
            Database.getInstance().releaseConnection(conn);
        }
    }
}
