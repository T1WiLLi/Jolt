package ca.jolt.database;

import ca.jolt.database.models.TableMetadata;
import ca.jolt.exceptions.DatabaseException;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;
import ca.jolt.utils.HelpMethods;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * A generic data-access class for an entity, providing common CRUD operations.
 * It leverages TableMetadata (for table/column reflection) and SchemaManager
 * (for schema creation/migration).
 */
public abstract class Broker<ID, T> {

    private static final Logger logger = Logger.getLogger(Broker.class.getName());

    protected final TableMetadata<T> metadata;
    protected final Class<T> entityClass;
    protected final Field idField;

    protected Broker() {
        this.entityClass = resolveEntityClass();
        this.metadata = new TableMetadata<>(entityClass);
        this.idField = metadata.getId();

        executeInTransaction(conn -> {
            SchemaManager.validateTable(conn, metadata);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveEntityClass() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) superClass;
            return (Class<T>) pt.getActualTypeArguments()[1];
        }
        throw new IllegalStateException(
                "Could not resolve entity class. Ensure your repository directly extends Broker.");
    }

    public Optional<T> findById(ID id) {
        return new Query<>(
                entityClass,
                "SELECT * FROM " + metadata.getTableName() + " WHERE " + metadata.getIdColumn() + " = ?",
                true,
                id).selectSingle();
    }

    public List<T> findAll() {
        return new Query<>(
                entityClass,
                "SELECT * FROM " + metadata.getTableName(),
                true).selectList();
    }

    public T save(T entity) {
        try {
            Object id = idField.get(entity);
            if (id == null) {
                return executeInTransaction(conn -> insert(entity, conn));
            } else {
                return executeInTransaction(conn -> {
                    try {
                        return update(entity, conn);
                    } catch (IllegalAccessException e) {
                        throw new DatabaseException("Error in update: " + e.getMessage(), e);
                    }
                });
            }
        } catch (IllegalAccessException e) {
            logger.severe(() -> "Error in save: " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reason());
        }
    }

    public boolean delete(T entity) {
        try {
            Object id = idField.get(entity);
            if (id != null) {
                executeInTransaction(conn -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM " + metadata.getTableName() + " WHERE " + metadata.getIdColumn() + " = ?")) {
                        ps.setObject(1, id);
                        ps.executeUpdate();
                        return true;
                    }
                });
            }
        } catch (IllegalAccessException e) {
            logger.severe(() -> "Error in delete: " + e.getMessage() + ", Stack trace: "
                    + HelpMethods.stackTraceElementToString(e.getStackTrace()));
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reason());
        }
        return false;
    }

    public boolean deleteById(ID id) {
        return executeInTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + metadata.getTableName() + " WHERE " + metadata.getIdColumn() + " = ?")) {
                ps.setObject(1, id);
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            }
        });
    }

    private T insert(T entity, Connection conn) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.equals(idField)) {
                continue;
            }

            String colName = metadata.getFieldToColumn().get(field.getName());
            if (colName == null) {
                continue;
            }
            columns.add(colName);

            try {
                field.setAccessible(true);
                values.add(field.get(entity));
            } catch (IllegalAccessException e) {
                logger.severe(() -> "Failed to access field " + field.getName() + ": " + e.getMessage());
                throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.reason());
            }
        }

        String colStr = String.join(", ", columns);
        String placeholders = String.join(", ", Collections.nCopies(values.size(), "?"));
        String sql = "INSERT INTO " + metadata.getTableName() + " (" + colStr + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                logger.warning(() -> "Insert may have failed; no rows affected");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Object generatedId = rs.getObject(1);
                    idField.setAccessible(true);
                    idField.set(entity, generatedId);
                }
            } catch (IllegalAccessException e) {
                logger.severe(() -> "Failed to set generated ID: " + e.getMessage() + ", Stack Trace: "
                        + HelpMethods.stackTraceElementToString(e.getStackTrace()));
                throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.reason());
            }
        }
        return entity;
    }

    private T update(T entity, Connection conn) throws SQLException, IllegalArgumentException, IllegalAccessException {
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.equals(idField)) {
                continue;
            }
            String colName = metadata.getFieldToColumn().get(field.getName());
            if (colName == null) {
                continue;
            }
            setClauses.add(colName + " = ?");

            field.setAccessible(true);
            values.add(field.get(entity));
        }

        String sql = "UPDATE " + metadata.getTableName() + " SET " + String.join(", ", setClauses)
                + " WHERE " + metadata.getIdColumn() + " = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            idField.setAccessible(true);
            ps.setObject(values.size() + 1, idField.get(entity));

            int affected = ps.executeUpdate();
            if (affected == 0) {
                logger.warning(() -> "Update may have failed; no rows affected");
            }
        }
        return entity;
    }

    protected Optional<T> selectSingle(String whereClause, Object... params) {
        return new Query<>(
                entityClass,
                "SELECT * FROM " + metadata.getTableName() + " " + whereClause,
                true,
                params).selectSingle();
    }

    protected List<T> select(String whereClause, Object... params) {
        return new Query<>(
                entityClass,
                "SELECT * FROM " + metadata.getTableName() + " " + whereClause,
                true,
                params).selectList();
    }

    protected int update(String sqlClause, Object... params) {
        return executeInTransaction(conn -> {
            String sql = "UPDATE " + metadata.getTableName() + " " + sqlClause;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps.executeUpdate();
            }
        });
    }

    protected int sql(String rawSql, Object... params) {
        boolean isSelect = rawSql.trim().toUpperCase().startsWith("SELECT");
        if (isSelect) {
            return new Query<>(entityClass, rawSql, true, params).executeUpdate();
        } else {
            return executeInTransaction(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(rawSql)) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                    return ps.executeUpdate();
                }
            });
        }
    }

    public long count(String whereClause, Object... params) {
        String sql = "SELECT COUNT(*) AS total FROM " + metadata.getTableName() + " " + whereClause;
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
            }
        } catch (SQLException e) {
            logger.severe(() -> "Error executing count : " + e.getMessage() + ", Stack Trace: "
                    + HelpMethods.stackTraceElementToString(e.getStackTrace()));
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reason());
        }
        return 0L;
    }

    public boolean exists(ID id) {
        return findById(id).isPresent();
    }

    protected <R> R executeInTransaction(TransactionCallback<R> callback) {
        try (Connection conn = Database.getInstance().getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                R result = callback.doInTransaction(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe(() -> "Failed to rollback transaction: " + rollbackEx.getMessage()
                            + ", Stack Trace: "
                            + HelpMethods.stackTraceElementToString(rollbackEx.getStackTrace()));
                    throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                            HttpStatus.INTERNAL_SERVER_ERROR.reason());
                }
                logger.severe(() -> "Transaction failed: " + e.getMessage() + ", Stack Trace: "
                        + HelpMethods.stackTraceElementToString(e.getStackTrace()));
                throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.reason());
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException resetEx) {
                    logger.warning("Failed to reset auto-commit: " + resetEx.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.severe(() -> "Error obtaining database connection: " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.reason());
        }
    }

    protected <R> R withTransaction(TransactionCallback<R> callback) {
        return executeInTransaction(callback);
    }

    @FunctionalInterface
    protected interface TransactionCallback<R> {
        R doInTransaction(Connection conn) throws SQLException;
    }
}
