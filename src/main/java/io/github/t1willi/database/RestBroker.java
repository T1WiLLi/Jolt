package io.github.t1willi.database;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.t1willi.database.exception.DatabaseErrorType;
import io.github.t1willi.database.exception.DatabaseException;
import io.github.t1willi.utils.StringUtils;

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
    private static final Logger logger = Logger.getLogger(RestBroker.class.getName());
    private final Class<ID> id;
    private final String idColumnName;

    protected RestBroker(String table, Class<T> entityClass, Class<ID> idClass, String idColumnName) {
        super(table, entityClass);
        this.id = idClass;
        this.idColumnName = idColumnName != null ? idColumnName : "id";
    }

    protected RestBroker(String table, Class<T> entityClass, Class<ID> idClass) {
        this(table, entityClass, idClass, "id");
    }

    public List<T> findAll() {
        return selectMany("SELECT * FROM " + table);
    }

    public List<T> findAll(int offset, int limit) {
        return selectMany("SELECT * FROM " + table + " LIMIT ? OFFSET ?", limit, offset);
    }

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

    public Optional<T> findById(ID id) {
        if (id == null) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "The ID is null! Please change it.",
                    "The ID was found null, if you believe this to be an error, please reach out on github.", null);
        }
        return selectOne("SELECT * FROM " + table + " WHERE " + idColumnName + " = ?", id);
    }

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

    public int deleteById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        String sql = "DELETE FROM " + table + " WHERE " + idColumnName + " = ?";
        return query(sql, id);
    }

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

        int rows = query(sql.toString(), params);
        if (rows == 0) {
            insertEntity(entity);
        }
    }

    private ID insertEntity(T entity) {
        Map<String, Object> fields = extractFields(entity);
        boolean idProvided = fields.containsKey(idColumnName) && !isEmptyId(fields.get(idColumnName));
        if (!idProvided) {
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
            ID newId;

            if (idProvided) {
                try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                    int idx = 1;
                    for (Object val : fields.values()) {
                        stmt.setObject(idx++, val);
                    }
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                                "Creating entity failed, no rows affected",
                                "The database did not insert the entity",
                                null);
                    }
                    newId = id.cast(fields.get(idColumnName));
                }
            } else {
                try (PreparedStatement stmt = connection.prepareStatement(
                        sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                    int idx = 1;
                    for (Object val : fields.values()) {
                        stmt.setObject(idx++, val);
                    }
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                                "Creating entity failed, no rows affected",
                                "The database did not insert the entity",
                                null);
                    }
                    try (ResultSet gen = stmt.getGeneratedKeys()) {
                        if (gen.next()) {
                            newId = convertToIdType(gen.getObject(1));
                        } else {
                            throw new DatabaseException(DatabaseErrorType.UNKNOWN_ERROR,
                                    "Creating entity failed, no ID obtained",
                                    "The database did not return a generated ID",
                                    null);
                        }
                    }
                }
            }

            try {
                Field idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                idField.set(entity, newId);
            } catch (Exception e) {
                logger.warning("Could not set ID on entity: " + e.getMessage());
            }

            connection.commit();
            return newId;
        } catch (SQLException e) {
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

    private Map<String, Object> extractFields(T entity) {
        Map<String, Object> fields = new HashMap<>();
        Class<?> entityClass = entity.getClass();

        while (entityClass != null && entityClass != Object.class) {
            for (Field field : entityClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) ||
                        Modifier.isFinal(modifiers) ||
                        Modifier.isTransient(modifiers)) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);

                    if (value != null && isScalarValue(value)) {
                        String dbColumnName = StringUtils.camelToSnakeCase(field.getName());
                        fields.put(dbColumnName, value);
                    }
                } catch (IllegalAccessException e) {
                    logger.warning(() -> "Could not access field: " + field.getName() + " - " + e.getMessage());
                }
            }
            entityClass = entityClass.getSuperclass();
        }
        return fields;
    }

    private boolean isScalarValue(Object value) {
        return !(value instanceof Collection<?> ||
                value instanceof Map<?, ?> ||
                value.getClass().isArray());
    }

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
}