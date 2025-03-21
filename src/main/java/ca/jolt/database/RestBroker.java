package ca.jolt.database;

import java.lang.reflect.Field;
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
 * Defines REST FUNCTION ALITY for the database.
 * Built on-top of the Broker class.
 * 
 */
public abstract class RestBroker<ID, T> extends Broker<T> {
    private static final Logger logger = Logger.getLogger(RestBroker.class.getName());

    private final Class<ID> id;
    private final String idColumnName;

    /**
     * Creates a new RestBroker for the specified entity type and table.
     *
     * @param table        The database table name
     * @param entityClass  The entity class this broker manages
     * @param idClass      The class of the ID field
     * @param idColumnName The name of the ID column in the database (defaults to
     *                     "id" if not specified)
     */
    protected RestBroker(String table, Class<T> entityClass, Class<ID> idClass, String idColumnName) {
        super(table, entityClass);
        this.id = idClass;
        this.idColumnName = idColumnName != null ? idColumnName : "id";
    }

    /**
     * Creates a new RestBroker with default "id" column name.
     *
     * @param table       The database table name
     * @param entityClass The entity class this broker manages
     * @param idClass     The class of the ID field
     */
    protected RestBroker(String table, Class<T> entityClass, Class<ID> idClass) {
        this(table, entityClass, idClass, "id");
    }

    // Define function REST.

    protected List<T> findAll() {
        return selectMany("SELECT * FROM " + table);
    }

    protected List<T> findAll(int offset, int limit) {
        return selectMany("SELECT * FROM " + table + " LIMIT ? OFFSET ?", limit, offset);
    }

    protected List<T> findByCriteria(Map<String, Object> criteria) {
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

    protected Optional<T> findById(ID id) {
        if (id == null) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The ID is null ! Please change it.",
                    "The ID was found null, if you believe this to be an error, please reach out on github.", null);
        }
        return selectOne("SELECT * FROM " + table + " WHERE " + idColumnName + " = ?", id);
    }

    @SuppressWarnings("unchecked")
    public ID save(T entity) {
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

            ID id = (ID) idField.get(entity);

            if (id == null || isEmptyId(id)) {
                return insert(entity);
            } else {
                update(entity);
                return id;
            }
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Could not access ID field", e);
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected error occurred while saving the entity",
                    "Reflection error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Deletes an entity from the database.
     *
     * @param entity The entity to delete
     * @return true if deletion was successful, false otherwise
     * @throws IllegalArgumentException if entity is null
     */
    @SuppressWarnings("unchecked")
    public boolean delete(T entity) {
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
            ID id = (ID) idField.get(entity);

            if (id == null || isEmptyId(id)) {
                return false;
            }

            return deleteById(id);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Could not access ID field", e);
            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                    "An unexpected error occurred while deleting the entity",
                    "Reflection error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id The ID of the entity to delete
     * @return true if deletion was successful, false otherwise
     * @throws IllegalArgumentException if ID is null
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
     * @param entity The entity to update
     * @return The number of rows affected
     */
    @SuppressWarnings("unchecked")
    private ID update(T entity) {
        Map<String, Object> fields = extractFields(entity);
        ID id = (ID) fields.remove(idColumnName); // Remove ID from fields to update

        if (fields.isEmpty()) {
            return id;
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
        params[i] = id;

        query(sql.toString(), params);
        return id;
    }

    /**
     * Inserts a new entity into the database.
     *
     * @param entity The entity to insert
     * @return The ID of the newly inserted entity
     */
    @SuppressWarnings("unchecked")
    private ID insert(T entity) {
        Map<String, Object> fields = extractFields(entity);

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
                        ID newId = convertToIdType(generatedKeys.getObject(1));

                        Field idField = getIdField(entity.getClass());
                        if (idField != null) {
                            idField.setAccessible(true);
                            idField.set(entity, newId);
                        }

                        connection.commit();
                        return newId;
                    } else {
                        connection.commit();
                        return fields.containsKey(idColumnName) ? (ID) fields.get(idColumnName) : null;
                    }
                }
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
     * Extracts field values from an entity using reflection.
     * 
     * @param entity The entity to extract fields from
     * @return A map of field names to values
     */
    private Map<String, Object> extractFields(T entity) {
        Map<String, Object> fields = new HashMap<>();

        Class<?> currentClass = entity.getClass();
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(entity);
                    fields.put(field.getName(), value);
                } catch (IllegalAccessException e) {
                    logger.severe(() -> "Could not access field: " + field.getName() + " - " + e.getMessage());
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    /**
     * Finds the field in the entity class that matches the ID column name.
     * 
     * @param clazz The entity class
     * @return The ID field, or null if not found
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
     * Checks if an ID value is empty (null, 0, or empty string).
     * 
     * @param id The ID to check
     * @return true if the ID is empty, false otherwise
     */
    private boolean isEmptyId(ID id) {
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
     * Converts a database object to the appropriate ID type.
     * 
     * @param dbValue The database value to convert
     * @return The converted ID value
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
     * Rolls back a transaction silently.
     * 
     * @param conn The connection to roll back
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
     * Resets connection state and releases it back to the pool.
     * 
     * @param conn The connection to reset and release
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