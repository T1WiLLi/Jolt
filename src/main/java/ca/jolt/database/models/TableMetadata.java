package ca.jolt.database.models;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.GenerationType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.Table;
import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;
import lombok.Getter;

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
    }

    /**
     * Finds the field in the given class that is annotated with @Id.
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
     * Builds the map of fieldName -> columnName by looking for @Column annotation
     * or falling back to a snake_case version of the field name.
     */
    private static Map<String, String> buildFieldToColumnMap(Class<?> clazz) {
        Map<String, String> map = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Column col = field.getAnnotation(Column.class);
            String colName = (col != null && !col.value().isEmpty())
                    ? col.value()
                    : toSnakeCase(field.getName());
            map.put(field.getName(), colName);
        }
        return map;
    }

    /**
     * Builds the map of columnName -> Field so we can easily set/get values
     * when mapping rows from the DB to objects or vice versa.
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
     * Converts a Java class name into snake_case for DB columns/tables.
     */
    private static String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
