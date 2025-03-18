package ca.jolt.database.models;

import ca.jolt.database.annotation.*;
import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Stores metadata about an entity's table, columns, relationships, etc.
 */
@Getter
public class TableMetadata<T> {

    private final Class<T> entityClass;
    private final String tableName;

    private final Field id;
    private final String idColumn;
    private final GenerationType idGenerationType;
    private final String sequencePrefix;

    private final Map<String, String> fieldToColumn;
    private final Map<String, Field> columnToField;

    private final List<String> unique;
    private final List<String> indexes;

    private final Map<String, RelationMetadata> relationships;
    private final List<String> joinTables;

    public TableMetadata(Class<T> entityClass) {
        this.entityClass = entityClass;

        Table tableAnno = entityClass.getAnnotation(Table.class);
        this.tableName = (tableAnno != null && !tableAnno.table().isEmpty())
                ? tableAnno.table()
                : toSnakeCase(entityClass.getSimpleName());

        this.id = findIdField(entityClass);
        Id idAnnotation = this.id.getAnnotation(Id.class);
        this.idGenerationType = idAnnotation.generationType();
        this.sequencePrefix = idAnnotation.sequence();

        Column idColumnAnno = this.id.getAnnotation(Column.class);
        this.idColumn = (idColumnAnno != null && !idColumnAnno.value().isEmpty())
                ? idColumnAnno.value()
                : toSnakeCase(this.id.getName());

        this.fieldToColumn = buildFieldToColumnMap(entityClass);
        this.columnToField = buildColumnToFieldMap(entityClass, fieldToColumn);

        this.unique = (tableAnno != null) ? Arrays.asList(tableAnno.unique()) : Collections.emptyList();
        this.indexes = (tableAnno != null) ? Arrays.asList(tableAnno.indexes()) : Collections.emptyList();

        this.relationships = buildRelationshipMap(entityClass);
        this.joinTables = buildJoinTables(entityClass);
    }

    /**
     * Finds the field annotated with @Id (single primary key).
     */
    private static Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalStateException("No @Id field found in class " + clazz.getName());
    }

    /**
     * Builds fieldName->columnName by checking @Column or falling back to
     * snake_case.
     * For relationship fields (ManyToOne, etc.), we look for @JoinColumn if
     * present.
     */
    private static Map<String, String> buildFieldToColumnMap(Class<?> clazz) {
        Map<String, String> map = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            if (isRelationshipField(field) && !field.isAnnotationPresent(JoinColumn.class)) {
                continue;
            }

            if (field.isAnnotationPresent(JoinColumn.class)) {
                JoinColumn joinCol = field.getAnnotation(JoinColumn.class);
                map.put(field.getName(), joinCol.value());
                continue;
            }

            Column col = field.getAnnotation(Column.class);
            String colName = (col != null && !col.value().isEmpty())
                    ? col.value()
                    : toSnakeCase(field.getName());

            map.put(field.getName(), colName);
        }
        return map;
    }

    /**
     * Builds columnName->Field map for easy reflection-based data mapping.
     */
    private static Map<String, Field> buildColumnToFieldMap(Class<?> clazz, Map<String, String> fieldToColumn) {
        Map<String, Field> map = new HashMap<>();
        fieldToColumn.forEach((fieldName, columnName) -> {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                map.put(columnName, f);
            } catch (NoSuchFieldException e) {
                throw new DatabaseException(
                        DatabaseErrorType.DATA_INTEGRITY_ERROR,
                        "Error building column->field map for field " + fieldName,
                        e.getMessage(),
                        e);
            }
        });
        return map;
    }

    /**
     * Builds a map of relationship fields (OneToMany, ManyToOne, ManyToMany).
     */
    private Map<String, RelationMetadata> buildRelationshipMap(Class<?> clazz) {
        Map<String, RelationMetadata> relationMap = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (!isRelationshipField(field)) {
                continue;
            }

            RelationMetadata rm = new RelationMetadata();
            rm.field = field;

            if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany anno = field.getAnnotation(OneToMany.class);
                rm.type = RelationType.ONE_TO_MANY;
                rm.mappedBy = anno.mappedBy();
                rm.fetchType = anno.fetch();
                rm.cascadeTypes = anno.cascade();
                rm.orphanRemoval = anno.orphanRemoval();

                if (Collection.class.isAssignableFrom(field.getType())) {
                    ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                    rm.targetEntity = (Class<?>) paramType.getActualTypeArguments()[0];
                } else {
                    throw new DatabaseException(
                            DatabaseErrorType.MAPPING_ERROR,
                            "@OneToMany field must be a Collection: " + field.getName(),
                            "Invalid collection type",
                            null);
                }
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne anno = field.getAnnotation(ManyToOne.class);
                rm.type = RelationType.MANY_TO_ONE;
                rm.mappedBy = anno.mappedBy();
                rm.fetchType = anno.fetch();
                rm.targetEntity = field.getType();

                if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn jc = field.getAnnotation(JoinColumn.class);
                    rm.joinColumnName = jc.value();
                    rm.nullable = jc.nullable();
                } else {
                    rm.joinColumnName = toSnakeCase(field.getName()) + "_id";
                }
            } else if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany anno = field.getAnnotation(ManyToMany.class);
                rm.type = RelationType.MANY_TO_MANY;
                rm.mappedBy = anno.mappedBy();
                rm.fetchType = anno.fetch();
                rm.cascadeTypes = anno.cascade();

                if (Collection.class.isAssignableFrom(field.getType())) {
                    ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                    rm.targetEntity = (Class<?>) paramType.getActualTypeArguments()[0];
                } else {
                    throw new DatabaseException(
                            DatabaseErrorType.MAPPING_ERROR,
                            "@ManyToMany field must be a Collection: " + field.getName(),
                            "Invalid collection type",
                            null);
                }

                if (field.isAnnotationPresent(JoinTable.class)) {
                    JoinTable jt = field.getAnnotation(JoinTable.class);
                    rm.joinTable = jt.name();
                    if (rm.joinTable.isEmpty()) {
                        rm.joinTable = this.tableName + "_" + toSnakeCase(rm.targetEntity.getSimpleName());
                    }

                    if (jt.joinColumns().length > 0) {
                        rm.joinColumn = jt.joinColumns()[0].value();
                        rm.nullable = jt.joinColumns()[0].nullable();
                    } else {
                        rm.joinColumn = toSnakeCase(this.entityClass.getSimpleName()) + "_id";
                    }

                    if (jt.inverseJoinColumns().length > 0) {
                        rm.inverseJoinColumn = jt.inverseJoinColumns()[0].value();
                    } else {
                        rm.inverseJoinColumn = toSnakeCase(rm.targetEntity.getSimpleName()) + "_id";
                    }
                } else {
                    rm.joinTable = anno.joinTable();
                    rm.joinColumn = anno.joinColumn();
                    rm.inverseJoinColumn = anno.inverseJoinColumn();

                    if (rm.joinTable.isEmpty()) {
                        rm.joinTable = this.tableName + "_" + toSnakeCase(rm.targetEntity.getSimpleName());
                    }
                    if (rm.joinColumn.isEmpty()) {
                        rm.joinColumn = toSnakeCase(this.entityClass.getSimpleName()) + "_id";
                    }
                    if (rm.inverseJoinColumn.isEmpty()) {
                        rm.inverseJoinColumn = toSnakeCase(rm.targetEntity.getSimpleName()) + "_id";
                    }
                }
            }

            relationMap.put(field.getName(), rm);
        }

        return relationMap;
    }

    /**
     * Builds a list of join table names for ManyToMany relationships (owner side).
     */
    private List<String> buildJoinTables(Class<?> clazz) {
        List<String> tables = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany anno = field.getAnnotation(ManyToMany.class);
                if (!anno.mappedBy().isEmpty()) {
                    continue;
                }

                String joinTableName;
                if (field.isAnnotationPresent(JoinTable.class)) {
                    JoinTable jt = field.getAnnotation(JoinTable.class);
                    joinTableName = jt.name();
                    if (joinTableName.isEmpty()) {
                        Class<?> targetEntity = getCollectionGeneric(field);
                        joinTableName = this.tableName + "_" + toSnakeCase(targetEntity.getSimpleName());
                    }
                } else {
                    joinTableName = anno.joinTable();
                    if (joinTableName.isEmpty()) {
                        Class<?> targetEntity = getCollectionGeneric(field);
                        joinTableName = this.tableName + "_" + toSnakeCase(targetEntity.getSimpleName());
                    }
                }

                tables.add(joinTableName);
            }
        }
        return tables;
    }

    /**
     * Helper to get the generic type for a Collection field.
     */
    private Class<?> getCollectionGeneric(Field field) {
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        return (Class<?>) paramType.getActualTypeArguments()[0];
    }

    private static boolean isRelationshipField(Field field) {
        return field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    private static String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Getter
    public static class RelationMetadata {
        private Field field;
        private RelationType type;
        private Class<?> targetEntity;
        private String mappedBy = "";
        private FetchType fetchType;
        private CascadeType[] cascadeTypes = new CascadeType[0];
        private boolean orphanRemoval = false;

        // For ManyToOne or futur OneToOne
        private String joinColumnName;
        private boolean nullable = true;

        // For ManyToMany
        private String joinTable;
        private String joinColumn;
        private String inverseJoinColumn;
    }

    public enum RelationType {
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }
}
