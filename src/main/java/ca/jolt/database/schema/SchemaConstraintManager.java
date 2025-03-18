package ca.jolt.database.schema;

import ca.jolt.database.models.TableMetadata;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Responsible for handling constraints: check constraints, enum constraints,
 * updating column type, nullability, etc.
 */
public final class SchemaConstraintManager {
    private static final Logger logger = Logger.getLogger(SchemaConstraintManager.class.getName());

    private SchemaConstraintManager() {
        // Private constructor
    }

    /**
     * Loads all constraints from the database (e.g. for @CheckEnum)
     * and registers them with the CheckEnumConstraintRegistry or similar.
     */
    public static void loadAllConstraints(Connection conn, String tableName) {
        try {
            String sql = "SELECT constraint_name, check_clause " +
                    "FROM information_schema.check_constraints " +
                    "JOIN information_schema.constraint_column_usage USING (constraint_name) " +
                    "WHERE constraint_name LIKE ? AND constraint_name LIKE '%_check'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, tableName + "_%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String constraintName = rs.getString("constraint_name");
                        String checkClause = rs.getString("check_clause");

                        if (checkClause.contains("ANY (ARRAY[")) {
                            SchemaUtils.extractAndRegisterCheckValues(
                                    constraintName, checkClause, "ARRAY\\[(.+?)\\]");
                        } else if (checkClause.contains(" IN (")) {
                            SchemaUtils.extractAndRegisterCheckValues(
                                    constraintName, checkClause, "IN \\((.+?)\\)");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load check constraints for table " + tableName + ": " + e.getMessage());
        }
    }

    /**
     * Called by SchemaUpdater to update constraints (check, enum, nullability,
     * column type).
     */
    public static <T> void updateConstraints(Connection conn,
            TableMetadata<T> metadata,
            Map<String, Boolean> existingColumns) throws SQLException {
        Map<String, Set<String>> existingConstraints = SchemaUtils.getExistingConstraints(conn,
                metadata.getTableName());
        Map<String, String> existingColumnTypes = SchemaUtils.getExistingColumnTypes(conn, metadata.getTableName());
        Map<String, Boolean> existingNullableStatus = SchemaUtils.getExistingNullableStatus(conn,
                metadata.getTableName());

        for (Field field : metadata.getEntityClass().getDeclaredFields()) {
            String colName = metadata.getFieldToColumn().get(field.getName());
            if (colName == null || !existingColumns.containsKey(colName.toLowerCase())) {
                continue;
            }
            SchemaUtils.updateCheckConstraint(conn, metadata, existingConstraints, field, colName);
            SchemaUtils.updateCheckEnumConstraint(conn, metadata, existingConstraints, field, colName);
            SchemaUtils.updateNullableConstraint(conn, metadata, existingNullableStatus, field, colName);
            SchemaUtils.updateColumnType(conn, metadata, existingColumnTypes, field, colName);
        }
    }
}
