package ca.jolt.database.schema;

import ca.jolt.database.models.TableMetadata;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Responsible for updating an existing table schema: adding/removing columns,
 * altering constraints, etc.
 */
public final class SchemaUpdater {
    private static final Logger logger = Logger.getLogger(SchemaUpdater.class.getName());

    private SchemaUpdater() {
        // Private constructor
    }

    /**
     * Updates the existing table schema to match the entity definition.
     */
    public static <T> void updateTableSchema(Connection conn, TableMetadata<T> metadata) throws SQLException {
        Map<String, Boolean> existingColumns = SchemaUtils.getExistingColumns(conn, metadata.getTableName());
        Set<String> entityColumns = SchemaUtils.getEntityColumnNames(metadata);

        addMissingColumns(conn, metadata, existingColumns);
        removeExtraColumns(conn, metadata, existingColumns, entityColumns);
        SchemaConstraintManager.updateConstraints(conn, metadata, existingColumns);
    }

    /**
     * Adds columns present in the entity but not in the DB table.
     */
    private static <T> void addMissingColumns(Connection conn,
            TableMetadata<T> metadata,
            Map<String, Boolean> existingColumns) throws SQLException {
        for (Field field : metadata.getEntityClass().getDeclaredFields()) {
            String colName = metadata.getFieldToColumn().get(field.getName());
            if (colName == null) {
                continue;
            }
            if (!existingColumns.containsKey(colName.toLowerCase())) {
                String columnDef = SchemaUtils.buildColumnDefinition(field, metadata);
                columnDef = columnDef.replace(" PRIMARY KEY", "");
                columnDef = columnDef.replace(" GENERATED ALWAYS AS IDENTITY", "");

                String alterSql = "ALTER TABLE " + metadata.getTableName() + " ADD COLUMN " + columnDef;
                SchemaUtils.executeStatement(conn, alterSql,
                        "Added column " + colName + " to table " + metadata.getTableName(),
                        metadata.getTableName());
            }
        }
    }

    /**
     * Removes columns that are in the DB table but not in the entity definition.
     */
    private static <T> void removeExtraColumns(Connection conn,
            TableMetadata<T> metadata,
            Map<String, Boolean> existingColumns,
            Set<String> entityColumns) throws SQLException {
        for (String colName : existingColumns.keySet()) {
            boolean isInEntity = entityColumns.contains(colName.toLowerCase());
            boolean isIdColumn = colName.equalsIgnoreCase(metadata.getIdColumn());
            if (!isInEntity && !isIdColumn) {
                String alterSql = "ALTER TABLE " + metadata.getTableName() + " DROP COLUMN " + colName;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterSql);
                    logger.info("Removed column " + colName + " from table " + metadata.getTableName());
                } catch (SQLException e) {
                    logger.warning("Could not remove column '" + colName + "': " + e.getMessage());
                }
            }
        }
    }
}
