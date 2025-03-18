package ca.jolt.database.schema;

import ca.jolt.database.annotation.Id;
import ca.jolt.database.models.TableMetadata;
import ca.jolt.database.annotation.GenerationType;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for creating brand-new tables (and sequences, if needed).
 */
public final class SchemaCreator {
    private SchemaCreator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a table based on the TableMetadata, including columns, primary key,
     * and unique constraints.
     */
    public static <T> void createTable(Connection conn, TableMetadata<T> metadata) throws SQLException {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(metadata.getTableName()).append(" (");

        List<String> columnDefs = new ArrayList<>();
        List<String> uniqueConstraints = new ArrayList<>();

        columnDefs.add(buildColumnDefinition(metadata.getId(), metadata));

        for (Field field : metadata.getEntityClass().getDeclaredFields()) {
            if (field.equals(metadata.getId())) {
                continue;
            }
            if (metadata.getFieldToColumn().containsKey(field.getName())) {
                columnDefs.add(SchemaUtils.buildColumnDefinition(field, metadata));
            }
        }

        for (String uniqueCol : metadata.getUnique()) {
            uniqueConstraints.add("CONSTRAINT uk_" + uniqueCol + " UNIQUE (" + uniqueCol + ")");
        }

        createSql.append(String.join(", ", columnDefs));
        if (!uniqueConstraints.isEmpty()) {
            createSql.append(", ").append(String.join(", ", uniqueConstraints));
        }
        createSql.append(")");

        createSequenceIfNeeded(conn, metadata);

        SchemaUtils.executeStatement(conn, createSql.toString(),
                "Created table: " + metadata.getTableName(),
                metadata.getTableName());
    }

    /**
     * Creates a sequence for ID generation if the entity uses
     * GenerationType.SEQUENCE.
     */
    private static <T> void createSequenceIfNeeded(Connection conn, TableMetadata<T> metadata) throws SQLException {
        if (metadata.getIdGenerationType() == GenerationType.SEQUENCE) {
            String seqSql = "CREATE SEQUENCE IF NOT EXISTS " + metadata.getTableName() + "_id_seq";
            SchemaUtils.executeStatement(conn, seqSql,
                    "Created sequence for table: " + metadata.getTableName(),
                    metadata.getTableName());
        }
    }

    /**
     * Similar to buildColumnDefinition, but for the PK field specifically.
     */
    private static <T> String buildColumnDefinition(Field pkField, TableMetadata<T> metadata) {
        String baseDefinition = SchemaUtils.buildColumnDefinition(pkField, metadata);
        if (pkField.isAnnotationPresent(Id.class)) {
            if (metadata.getIdGenerationType() == GenerationType.IDENTITY) {
                if (!baseDefinition.contains("GENERATED ALWAYS AS IDENTITY")) {
                    baseDefinition += " GENERATED ALWAYS AS IDENTITY";
                }
            }
        }
        return baseDefinition;
    }
}
