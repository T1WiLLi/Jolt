package ca.jolt.database;

import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.Table;
import ca.jolt.exceptions.DatabaseException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class Broker<ID, T> {

    private final Class<T> entityClass;
    private final String tableName;
    private final Field idField;
    private final String idColumnName;

    protected Broker() {
        this.entityClass = resolveEntityClass();
        Table tableAnno = entityClass.getAnnotation(Table.class);
        this.tableName = (tableAnno != null && !tableAnno.table().isEmpty())
                ? tableAnno.table()
                : toSnakeCase(entityClass.getSimpleName());
        this.idField = resolveIdField();
        this.idColumnName = toSnakeCase(idField.getName());
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

    private Field resolveIdField() {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalStateException("Entity " + entityClass.getName() + " does not have an @Id annotated field.");
    }

    private String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // --- Basic CRUD Operations ---

    public Optional<T> findById(ID id) {
        return new Query<>(entityClass, "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?", true, id)
                .selectSingle();
    }

    public List<T> findAll() {
        return new Query<>(entityClass, "SELECT * FROM " + tableName, true).selectList();
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
            throw new DatabaseException("Error in save: " + e.getMessage(), e);
        }
    }

    public boolean delete(T entity) {
        try {
            Object id = idField.get(entity);
            if (id != null) {
                executeInTransaction(conn -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?")) {
                        ps.setObject(1, id);
                        ps.executeUpdate();
                        return true;
                    }
                });
            }
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Error in delete", e);
        }
        return false;
    }

    public boolean deleteById(ID id) {
        return executeInTransaction(conn -> {
            try (PreparedStatement ps = conn
                    .prepareStatement("DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?")) {
                ps.setObject(1, id);
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            }
        });
    }

    // --- Private Insert/Update Implementation with Transaction Support ---

    private T insert(T entity, Connection conn) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.equals(idField))
                continue;
            columns.add(toSnakeCase(field.getName()));
            try {
                values.add(field.get(entity));
            } catch (IllegalAccessException e) {
                throw new SQLException("Failed to access field " + field.getName(), e);
            }
        }
        String colStr = String.join(", ", columns);
        String placeholders = String.join(", ", Collections.nCopies(values.size(), "?"));
        String sql = "INSERT INTO " + tableName + " (" + colStr + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Object generatedId = rs.getObject(1);
                    idField.set(entity, generatedId);
                }
            } catch (IllegalAccessException e) {
                throw new SQLException("Failed to set generated ID", e);
            }
        }
        return entity;
    }

    private T update(T entity, Connection conn) throws SQLException, IllegalArgumentException, IllegalAccessException {
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.equals(idField))
                continue;
            setClauses.add(toSnakeCase(field.getName()) + " = ?");
            try {
                values.add(field.get(entity));
            } catch (IllegalAccessException e) {
                throw new SQLException("Failed to access field " + field.getName(), e);
            }
        }
        String sql = "UPDATE " + tableName + " SET " + String.join(", ", setClauses)
                + " WHERE " + idColumnName + " = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.setObject(values.size() + 1, idField.get(entity));
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Update failed, no rows affected.");
            }
        }
        return entity;
    }

    // --- Protected Helper Methods for Custom Queries ---

    /**
     * Executes a custom SELECT query and returns a single Optional result.
     * Example: selectSingle("WHERE username = ?", username);
     */
    protected Optional<T> selectSingle(String whereClause, Object... params) {
        return new Query<>(entityClass, "SELECT * FROM " + tableName + " " + whereClause, true, params)
                .selectSingle();
    }

    /**
     * Executes a custom SELECT query and returns a list of results.
     * Example: select("WHERE age > ?", 20);
     */
    protected List<T> select(String whereClause, Object... params) {
        return new Query<>(entityClass, "SELECT * FROM " + tableName + " " + whereClause, true, params)
                .selectList();
    }

    /**
     * Executes an update or other modifying query using raw SQL within a
     * transaction.
     * Example: update("SET email = ? WHERE id = ?", newEmail, id);
     */
    protected int update(String sqlClause, Object... params) {
        return executeInTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE " + tableName + " " + sqlClause)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Executes a raw SQL query within a transaction if it's a modifying query.
     */
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

    /**
     * Convenience method to count rows in the table (optionally with a WHERE
     * clause).
     * Example: count("WHERE active = ?", true);
     */
    public long count(String whereClause, Object... params) {
        String sql = "SELECT COUNT(*) AS total FROM " + tableName + " " + whereClause;
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
            throw new DatabaseException("Error executing count", e);
        }
        return 0L;
    }

    /**
     * Checks if an entity exists by its ID.
     */
    public boolean exists(ID id) {
        return findById(id).isPresent();
    }

    /**
     * Generic method to execute any operation within a transaction.
     * All modifying operations automatically use this.
     */
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
                    throw new DatabaseException("Failed to rollback transaction: " + rollbackEx.getMessage(),
                            rollbackEx);
                }
                throw new DatabaseException("Transaction failed: " + e.getMessage(), e);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException resetEx) {
                    // Log warning but continue, as the connection is being closed anyway
                    System.err.println("Warning: Failed to reset auto-commit: " + resetEx.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obtaining database connection: " + e.getMessage(), e);
        }
    }

    /**
     * For backward compatibility - delegates to executeInTransaction
     */
    protected <R> R withTransaction(TransactionCallback<R> callback) {
        return executeInTransaction(callback);
    }

    @FunctionalInterface
    protected interface TransactionCallback<R> {
        R doInTransaction(Connection conn) throws SQLException;
    }
}