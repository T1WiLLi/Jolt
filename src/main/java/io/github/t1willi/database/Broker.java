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
            Map<String, Object> fieldMap = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String fieldName = StringUtils.snakeToCamelCase(columnName);
                int columnType = metaData.getColumnType(i);

                if (rs.getObject(i) == null) {
                    fieldMap.put(fieldName, null);
                    continue;
                }

                try {
                    Object value = null;
                    switch (columnType) {
                        case Types.TIMESTAMP:
                            value = rs.getTimestamp(i) != null ? rs.getTimestamp(i).toLocalDateTime() : null;
                            break;
                        case Types.DATE:
                            value = rs.getDate(i) != null ? rs.getDate(i).toLocalDate() : null;
                            break;
                        case Types.TIME:
                            value = rs.getTime(i) != null ? rs.getTime(i).toLocalTime() : null;
                            break;
                        case Types.NUMERIC:
                        case Types.DECIMAL:
                            value = rs.getBigDecimal(i);
                            break;
                        case Types.BOOLEAN:
                        case Types.BIT:
                            Object boolValue = rs.getObject(i);
                            if (boolValue instanceof Boolean) {
                                value = (Boolean) boolValue;
                            } else if (boolValue instanceof String) {
                                String strValue = (String) boolValue;
                                value = "t".equalsIgnoreCase(strValue) || "true".equalsIgnoreCase(strValue);
                            } else if (boolValue instanceof Number) {
                                value = ((Number) boolValue).intValue() != 0;
                            } else {
                                value = false;
                            }
                            break;
                        case Types.INTEGER:
                        case Types.SMALLINT:
                        case Types.TINYINT:
                            value = rs.getObject(i);
                            break;
                        default:
                            value = rs.getObject(i);
                            break;
                    }

                    fieldMap.put(fieldName, value);

                } catch (SQLException ex) {
                    fieldMap.put(fieldName, rs.getObject(i));
                }
            }
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