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

public abstract class Broker<K, T> {

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

    public Optional<T> findById(K id) {
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
                return insert(entity);
            } else {
                return update(entity);
            }
        } catch (IllegalAccessException | SQLException e) {
            throw new DatabaseException("Error in save: " + e.getMessage(), e);
        }
    }

    public void delete(T entity) {
        try {
            Object id = idField.get(entity);
            if (id != null) {
                new Query<>(entityClass, "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?", false, id)
                        .executeUpdate();
            }
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Error in delete", e);
        }
    }

    // --- Private Insert/Update Implementation ---

    private T insert(T entity) throws SQLException {
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
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    private T update(T entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
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
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
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
     * Executes an update or other modifying query using raw SQL.
     * Example: update("SET email = ? WHERE id = ?", newEmail, id);
     */
    protected int update(String sqlClause, Object... params) {
        return new Query<>(entityClass, "UPDATE " + tableName + " " + sqlClause, false, params)
                .executeUpdate();
    }

    /**
     * Executes a raw SQL query.
     */
    protected int sql(String rawSql, Object... params) {
        boolean isSelect = rawSql.trim().toUpperCase().startsWith("SELECT");
        return new Query<>(entityClass, rawSql, isSelect, params).executeUpdate();
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
    public boolean exists(K id) {
        return findById(id).isPresent();
    }

    /**
     * Executes the given callback within a transaction.
     * Commits if successful; otherwise, rolls back.
     */
    protected <R> R withTransaction(TransactionCallback<R> callback) {
        try (Connection conn = Database.getInstance().getConnection()) {
            try {
                conn.setAutoCommit(false);
                R result = callback.doInTransaction(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error handling transaction", e);
        }
    }

    @FunctionalInterface
    protected interface TransactionCallback<R> {
        R doInTransaction(Connection conn) throws SQLException;
    }
}
