package ca.jolt.database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;

/**
 * The `RestBroker` class is an abstract base class for managing CRUD operations
 * on database entities. It provides methods for finding, saving, updating, and
 * deleting entities, as well as querying by criteria. It uses reflection to
 * dynamically map database rows to entity objects and vice versa.
 *
 * @param <ID> The type of the entity's primary key.
 * @param <T>  The type of the entity.
 */
public abstract class RestBroker<ID, T> extends Broker<T> {
    private static final Logger logger = Logger.getLogger(RestBroker.class.getName()); // Logger for RestBroker
                                                                                       // operations
    private final Class<ID> id; // The class type of the entity's primary key
    private final String idColumnName; // The name of the primary key column in the database

    /**
     * Constructs a new `RestBroker` instance with the specified table name, entity
     * class,
     * primary key class, and primary key column name.
     *
     * @param table        The name of the database table.
     * @param entityClass  The class type of the entity.
     * @param idClass      The class type of the entity's primary key.
     * @param idColumnName The name of the primary key column in the database.
     */
    protected RestBroker(String table, Class<T> entityClass, Class<ID> idClass, String idColumnName) {
        super(table, entityClass);
        this.id = idClass;
        this.idColumnName = idColumnName != null ? idColumnName : "id";
    }

    /**
     * Constructs a new `RestBroker` instance with the specified table name, entity
     * class,
     * and primary key class. The primary key column name defaults to "id".
     *
     * @param table       The name of the database table.
     * @param entityClass The class type of the entity.
     * @param idClass     The class type of the entity's primary key.
     */
    protected RestBroker(String table, Class<T> entityClass, Class<ID> idClass) {
        this(table, entityClass, idClass, "id");
    }

    /**
     * Retrieves all entities from the database.
     *
     * @return A list of all entities.
     */
    public List<T> findAll() {
        return selectMany("SELECT * FROM " + table);
    }

    /**
     * Retrieves a paginated list of entities from the database.
     *
     * @param offset The number of entities to skip.
     * @param limit  The maximum number of entities to return.
     * @return A list of entities within the specified range.
     */
    public List<T> findAll(int offset, int limit) {
        return selectMany("SELECT * FROM " + table + " LIMIT ? OFFSET ?", limit, offset);
    }

    /**
     * Retrieves entities that match the specified criteria.
     *
     * @param criteria A map of field names to values to filter the results.
     * @return A list of entities that match the criteria.
     */
    public List<T> findByCriteria(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return findAll();
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table).append(" WHERE ");
        List<String> conditions = criteria.keySet().stream()
                .map(key -> key + " = ?")
                .toList();
        sql.append(String.join(" AND ", conditions));
        return selectMany(sql.toString(), criteria.values().toArray());
    }

    /**
     * Retrieves an entity by its primary key.
     *
     * @param id The primary key of the entity.
     * @return An `Optional` containing the entity if found, otherwise empty.
     * @throws DatabaseException If the ID is null.
     */
    public Optional<T> findById(ID id) {
        if (id == null) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The ID is null! Please change it.",
                    "The ID was found null, if you believe this to be an error, please reach out on github.", null);
        }
        return selectOne("SELECT * FROM " + table + " WHERE " + idColumnName + " = ?", id);
    }

    /**
     * Saves an entity to the database. If the entity has a null or empty ID, it is
     * inserted.
     * Otherwise, the existing entity is updated.
     *
     * @param entity The entity to save.
     * @return The saved entity.
     * @throws IllegalArgumentException If the entity is null.
     * @throws DatabaseException        If an error occurs during the save
     *                                  operation.
     */
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        try {
            Field idField = getIdField(entity.getClass());
            if (idField == null) {
                throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                        "Could not find ID field",
                        "Entity class does not have a field matching the ID column name: " + idColumnName,
                        null);
            }

            idField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ID entityId = (ID) idField.get(entity);

            if (entityId == null || isEmptyId(entityId)) {
                entityId = insertEntity(entity);
            } else {
                updateEntity(entity, entityId);
            }

            return findById(entityId).orElse(entity);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Could not access ID field", e);
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected error occurred while saving the entity",
                    "Reflection error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Deletes an entity by its primary key.
     *
     * @param id The primary key of the entity to delete.
     * @return `true` if the entity was deleted, otherwise `false`.
     * @throws IllegalArgumentException If the ID is null.
     */
    public boolean deleteById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        String sql = "DELETE FROM " + table + " WHERE " + idColumnName + " = ?";
        return query(sql, id);
    }

    /**
     * Updates an existing entity in the database.
     *
     * @param entity   The entity to update.
     * @param entityId The primary key of the entity.
     */
    private void updateEntity(T entity, ID entityId) {
        Map<String, Object> fields = extractFields(entity);
        fields.remove(idColumnName);

        if (fields.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");
        List<String> setStatements = fields.keySet().stream()
                .map(key -> key + " = ?")
                .collect(Collectors.toList());

        sql.append(String.join(", ", setStatements))
                .append(" WHERE ")
                .append(idColumnName)
                .append(" = ?");

        Object[] params = new Object[fields.size() + 1];
        int i = 0;
        for (Object value : fields.values()) {
            params[i++] = value;
        }
        params[i] = entityId;

        query(sql.toString(), params);
    }

    /**
     * Inserts a new entity into the database and returns its generated primary key.
     *
     * @param entity The entity to insert.
     * @return The generated primary key of the inserted entity.
     * @throws DatabaseException If the entity has no fields to insert or an error
     *                           occurs.
     */
    private ID insertEntity(T entity) {
        Map<String, Object> fields = extractFields(entity);

        if (fields.containsKey(idColumnName) && isEmptyId(fields.get(idColumnName))) {
            fields.remove(idColumnName);
        }

        if (fields.isEmpty()) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "Entity has no fields to insert",
                    "The entity must have at least one field to be inserted into the database",
                    null);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        sql.append(String.join(", ", fields.keySet()));
        sql.append(") VALUES (");
        sql.append(fields.values().stream().map(v -> "?").collect(Collectors.joining(", ")));
        sql.append(")");

        Connection connection = null;
        try {
            connection = Database.getInstance().getConnection();
            connection.setAutoCommit(false);
            ID newId = null;

            try (PreparedStatement stmt = connection.prepareStatement(
                    sql.toString(), Statement.RETURN_GENERATED_KEYS)) {

                int paramIndex = 1;
                for (Object value : fields.values()) {
                    stmt.setObject(paramIndex++, value);
                }

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                            "Creating entity failed, no rows affected",
                            "The database did not insert the entity",
                            null);
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newId = convertToIdType(generatedKeys.getObject(1));

                        String setterMethodName = "set" + idColumnName.substring(0, 1).toUpperCase()
                                + idColumnName.substring(1);
                        try {
                            Method setterMethod = entity.getClass().getMethod(setterMethodName, id);
                            setterMethod.invoke(entity, newId);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                                | InvocationTargetException e) {
                            logger.warning(
                                    "Could not invoke setter method: " + setterMethodName + " - " + e.getMessage());
                            Field idField = getIdField(entity.getClass());
                            if (idField != null) {
                                idField.setAccessible(true);
                                idField.set(entity, newId);
                            }
                        }
                    } else {
                        throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                                "Creating entity failed, no ID obtained",
                                "The database did not return a generated ID",
                                null);
                    }
                }
                connection.commit();
                return newId;
            }
        } catch (SQLException | IllegalAccessException e) {
            rollbackSilently(connection);
            logger.severe(() -> "Error during insert: " + e.getMessage());
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected database error occurred while inserting the entity",
                    "Error details: " + e.getMessage(),
                    e);
        } finally {
            resetConnectionAndRelease(connection);
        }
    }

    /**
     * Extracts the fields of an entity into a map of field names to values.
     *
     * @param entity The entity to extract fields from.
     * @return A map of field names to values.
     */
    private Map<String, Object> extractFields(T entity) {
        Map<String, Object> fields = new HashMap<>();
        Class<?> entityClass = entity.getClass();
        for (Method method : entityClass.getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !methodName.equals("getClass") && method.getParameterCount() == 0) {
                String fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                try {
                    Object value = method.invoke(entity);
                    if (value != null) {
                        fields.put(fieldName, value);
                    }
                } catch (Exception e) {
                    logger.severe(() -> "Could not invoke getter method: " + methodName + " - " + e.getMessage());
                }
            }
        }
        return fields;
    }

    /**
     * Retrieves the ID field of an entity class.
     *
     * @param clazz The entity class.
     * @return The ID field, or `null` if not found.
     */
    private Field getIdField(Class<?> clazz) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                Field field = currentClass.getDeclaredField(idColumnName);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(idColumnName)) {
                    return field;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    /**
     * Checks if an ID is considered empty (null, 0, or an empty string).
     *
     * @param id The ID to check.
     * @return `true` if the ID is empty, otherwise `false`.
     */
    private boolean isEmptyId(Object id) {
        if (id == null) {
            return true;
        }
        if (id instanceof Number) {
            return ((Number) id).longValue() == 0;
        }
        if (id instanceof String) {
            return ((String) id).isEmpty();
        }
        return false;
    }

    /**
     * Converts a database value to the appropriate ID type.
     *
     * @param dbValue The value from the database.
     * @return The converted ID.
     */
    @SuppressWarnings("unchecked")
    private ID convertToIdType(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        if (id.isAssignableFrom(dbValue.getClass())) {
            return (ID) dbValue;
        }
        if (id == Long.class || id == long.class) {
            return (ID) Long.valueOf(dbValue.toString());
        }
        if (id == Integer.class || id == int.class) {
            return (ID) Integer.valueOf(dbValue.toString());
        }
        if (id == String.class) {
            return (ID) dbValue.toString();
        }
        logger.warning("Unsupported ID type conversion: " + dbValue.getClass() + " to " + id);
        return (ID) dbValue;
    }

    /**
     * Rolls back a transaction silently, logging any errors that occur.
     *
     * @param conn The database connection to roll back.
     */
    private void rollbackSilently(Connection conn) {
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