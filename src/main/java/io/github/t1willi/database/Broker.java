package io.github.t1willi.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import io.github.t1willi.database.exception.DatabaseErrorType;
import io.github.t1willi.database.exception.DatabaseException;
import io.github.t1willi.utils.JacksonUtil;
import io.github.t1willi.utils.StringUtils;

/**
 * An abstract class that serves as a base for database brokers. It provides
 * common
 * functionality for executing SQL queries, mapping results to entities, and
 * handling
 * database connections and transactions.
 *
 * @param <T> The type of entity this broker handles.
 */
public abstract class Broker<T> {
    private static final Logger logger = Logger.getLogger(Broker.class.getName());

    /** The name of the database table associated with this broker. */
    protected final String table;

    /** The class type of the entity this broker handles. */
    protected final Class<T> entityClass;

    /**
     * Constructs a new Broker instance.
     *
     * @param table       The name of the database table associated with this
     *                    broker.
     * @param entityClass The class type of the entity this broker handles.
     * @throws DatabaseException If the table name is not a valid SQL identifier.
     */
    protected Broker(String table, Class<T> entityClass) {
        if (!SqlSecurity.isValidIdentifier(table)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The identifier for the class is not valid! Please change it.",
                    "The table name was found in our reserved keywords SQL-list. If you believe this to be an error, please report it as fast as possible to our github repository.",
                    null);
        }
        this.table = table;
        this.entityClass = entityClass;
    }

    /**
     * Executes a SQL query and returns a single result mapped to an entity.
     *
     * @param sql    The SQL query to execute.
     * @param params The parameters to bind to the SQL query.
     * @return An Optional containing the mapped entity if a result is found,
     *         otherwise empty.
     * @throws DatabaseException If the SQL query is invalid or a database error
     *                           occurs.
     */
    protected Optional<T> selectOne(String sql, Object... params) {
        if (!SqlSecurity.isValidRawSql(sql)) {
            System.out.println("The SQL query is invalid!");
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The SQL query is not valid! Please change it.",
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

    /**
     * Executes a SQL query and returns a list of results mapped to entities.
     *
     * @param sql    The SQL query to execute.
     * @param params The parameters to bind to the SQL query.
     * @return A list of mapped entities.
     * @throws DatabaseException If the SQL query is invalid or a database error
     *                           occurs.
     */
    protected List<T> selectMany(String sql, Object... params) {
        if (!SqlSecurity.isValidRawSql(sql)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The SQL query is not valid! Please change it.",
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

    /**
     * Executes a SQL update or insert query.
     *
     * @param sql    The SQL query to execute.
     * @param params The parameters to bind to the SQL query.
     * @return True if the query affected at least one row, otherwise false.
     * @throws DatabaseException If the SQL query is invalid or a database error
     *                           occurs.
     */
    protected boolean query(String sql, Object... params) {
        if (!SqlSecurity.isValidRawSql(sql)) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The SQL query is not valid! Please change it.",
                    "The SQL query was found to be invalid. If you believe this to be an error, please report it as fast as possible to our github repository.",
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

    /**
     * Retrieves the last inserted ID for the current connection after an INSERT
     * operation.
     * This method uses JDBC's standard `RETURN_GENERATED_KEYS` feature to retrieve
     * the generated key.
     *
     * @param stmt The PreparedStatement used to execute the INSERT query.
     * @return The last inserted ID, or -1 if no ID was generated or an error
     *         occurred.
     * @throws DatabaseException If a database error occurs.
     */
    protected long getReturningId(PreparedStatement stmt) {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            }
            return -1;
        } catch (SQLException e) {
            logger.severe(() -> "Failed to retrieve last inserted ID: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred while retrieving the last inserted ID.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Retrieves the number of rows affected by the last executed SQL update or
     * insert query.
     *
     * @param stmt The PreparedStatement used to execute the query.
     * @return The number of rows affected by the last query, or -1 if the count is
     *         unavailable.
     * @throws DatabaseException If a database error occurs.
     */
    protected int getLastAffectedRowCount(PreparedStatement stmt) {
        try {
            return stmt.getUpdateCount();
        } catch (SQLException e) {
            logger.severe(() -> "Failed to retrieve affected row count: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred. Please try again later.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Maps a ResultSet row to an entity of type T with improved type conversions.
     *
     * @param rs The ResultSet containing the data to map.
     * @return The mapped entity.
     * @throws SQLException      If a database access error occurs.
     * @throws DatabaseException If an error occurs during the mapping process.
     */
    protected T mapToEntity(ResultSet rs) throws SQLException {
        try {
            Map<String, Object> fieldMap = extractFieldMap(rs);
            return JacksonUtil.getObjectMapper().convertValue(fieldMap, entityClass);
        } catch (Exception e) {
            logger.severe(() -> "Error mapping ResultSet to entity: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred. Please try again later.",
                    "Unhandled database error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Extracts a map of field names and their corresponding values from a
     * ResultSet.
     *
     * @param rs The ResultSet containing the data to extract.
     * @return A map of field names to their values.
     * @throws SQLException If a database access error occurs.
     */
    private Map<String, Object> extractFieldMap(ResultSet rs) throws SQLException {
        Map<String, Object> fieldMap = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String fieldName = StringUtils.snakeToCamelCase(columnName);
            int columnType = metaData.getColumnType(i);

            Object value = mapColumnValue(rs, i, columnType);
            fieldMap.put(fieldName, value);
        }
        return fieldMap;
    }

    /**
     * Maps a column value from the ResultSet based on its SQL type.
     *
     * @param rs          The ResultSet containing the data.
     * @param columnIndex The index of the column to map.
     * @param columnType  The SQL type of the column.
     * @return The mapped value.
     * @throws SQLException If a database access error occurs.
     */
    private Object mapColumnValue(ResultSet rs, int columnIndex, int columnType) throws SQLException {
        if (rs.getObject(columnIndex) == null) {
            return null;
        }

        switch (columnType) {
            case Types.TIMESTAMP:
                return rs.getTimestamp(columnIndex) != null ? rs.getTimestamp(columnIndex).toLocalDateTime() : null;
            case Types.DATE:
                return rs.getDate(columnIndex) != null ? rs.getDate(columnIndex).toLocalDate() : null;
            case Types.TIME:
                return rs.getTime(columnIndex) != null ? rs.getTime(columnIndex).toLocalTime() : null;
            case Types.NUMERIC:
            case Types.DECIMAL:
                return rs.getBigDecimal(columnIndex);
            case Types.BOOLEAN:
            case Types.BIT:
                return mapBooleanValue(rs.getObject(columnIndex));
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return rs.getObject(columnIndex);
            default:
                return rs.getObject(columnIndex);
        }
    }

    /**
     * Maps a value to a boolean based on its type.
     *
     * @param value The value to map.
     * @return The mapped boolean value.
     */
    private Object mapBooleanValue(Object value) {
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof String) {
            String strValue = (String) value;
            return "t".equalsIgnoreCase(strValue) || "true".equalsIgnoreCase(strValue);
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else {
            return false;
        }
    }

    /**
     * Prepares a SQL statement with the given parameters.
     *
     * @param conn   The database connection to use.
     * @param sql    The SQL query to prepare.
     * @param params The parameters to bind to the SQL query.
     * @return A PreparedStatement object.
     * @throws SQLException If a database access error occurs.
     */
    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt;
        } catch (SQLException e) {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException closeEx) {
                    logger.warning(() -> "Failed to close PreparedStatement: " + closeEx.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * Rolls back a transaction silently, logging any errors that occur.
     *
     * @param conn The database connection to roll back.
     */
    void rollbackSilently(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            logger.warning(() -> "Failed to rollback transaction: " + e.getMessage());
        }
    }

    /**
     * Resets the connection's auto-commit state and releases it back to the pool.
     *
     * @param conn The database connection to reset and release.
     */
    void resetConnectionAndRelease(Connection conn) {
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