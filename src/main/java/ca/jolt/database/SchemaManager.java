package ca.jolt.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.GenerationType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.models.TableMetadata;
import ca.jolt.exceptions.DatabaseException;

public final class SchemaManager {
    private static final Logger logger = Logger.getLogger(SchemaManager.class.getName());

    public static <T> void validateTable(Connection conn, TableMetadata<T> metadata) {
        try {
            if (!tableExists(conn, metadata.getTableName())) {
                createTable(conn, metadata);
            } else {
                updateTableSchema(conn, metadata);
            }
            manageIndexes(conn, metadata);
        } catch (SQLException e) {
            logger.severe(() -> "Error validating table schema: " + e.getMessage());
            throw new DatabaseException("Failed to validate database schema", e);
        }
    }

    private static <T> void createTable(Connection conn, TableMetadata<T> metadata) throws SQLException {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(metadata.getTableName()).append(" (");

        List<String> columnDefs = new ArrayList<>();
        List<String> uniqueConstraints = new ArrayList<>();

        columnDefs.add(buildColumnDefinition(metadata.getId(), metadata));

        for (Field field : metadata.getEntityClass().getDeclaredFields()) {
            if (field.equals(metadata.getId()))
                continue;
            if (metadata.getFieldToColumn().containsKey(field.getName())) {
                columnDefs.add(buildColumnDefinition(field, metadata));
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

        if (metadata.getIdGenerationType() == GenerationType.SEQUENCE) {
            String seqSql = "CREATE SEQUENCE IF NOT EXISTS " + metadata.getTableName() + "_id_seq";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(seqSql);
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSql.toString());
            logger.info(() -> "Created table: " + metadata.getTableName());
        }
    }

    /**
     * Builds the DDL piece for a single column, including PK/Identity if needed.
     */
    private static <T> String buildColumnDefinition(Field field, TableMetadata<T> metadata) {
        Column columnAnno = field.getAnnotation(Column.class);
        String columnName = metadata.getFieldToColumn().get(field.getName());

        String sqlType = DatabaseUtils.getSqlTypeForJavaType(field.getType(), columnAnno);

        StringBuilder sb = new StringBuilder();
        sb.append(columnName).append(" ").append(sqlType);

        if (columnAnno != null && !columnAnno.nullable()) {
            sb.append(" NOT NULL");
        }

        if (field.isAnnotationPresent(Id.class)) {
            sb.append(" PRIMARY KEY");

            if (metadata.getIdGenerationType() == GenerationType.IDENTITY) {
                sb.append(" GENERATED ALWAYS AS IDENTITY");
            }
        }

        return sb.toString();
    }

    /**
     * Naive example: checks for missing columns and adds them. Real world usage may
     * need
     * more sophisticated checks (type changes, constraints, etc).
     */
    private static <T> void updateTableSchema(Connection conn, TableMetadata<T> metadata) throws SQLException {
        logger.info("Checking for missing columns for table: " + metadata.getTableName());

        DatabaseMetaData meta = conn.getMetaData();
        Map<String, Boolean> existingColumns = new HashMap<>();

        try (ResultSet rs = meta.getColumns(null, null, metadata.getTableName().toLowerCase(), null)) {
            while (rs.next()) {
                existingColumns.put(rs.getString("COLUMN_NAME").toLowerCase(), true);
            }
        }

        for (Field field : metadata.getEntityClass().getDeclaredFields()) {
            String colName = metadata.getFieldToColumn().get(field.getName());
            if (colName == null)
                continue;

            if (!existingColumns.containsKey(colName.toLowerCase())) {
                String columnDef = buildColumnDefinition(field, metadata);

                columnDef = columnDef.replace(" PRIMARY KEY", "");
                columnDef = columnDef.replace(" GENERATED ALWAYS AS IDENTITY", "");

                String alterSql = "ALTER TABLE " + metadata.getTableName() + " ADD COLUMN " + columnDef;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterSql);
                    logger.info("Added column " + colName + " to table " + metadata.getTableName());
                }
            }
        }
    }

    private static <T> void manageIndexes(Connection conn, TableMetadata<T> metadata) throws SQLException {
        for (String indexCol : metadata.getIndexes()) {
            String indexName = "idx_" + metadata.getTableName() + "_" + indexCol;
            String createIdxSql = "CREATE INDEX IF NOT EXISTS " + indexName
                    + " ON " + metadata.getTableName() + " (" + indexCol + ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createIdxSql);
                logger.info(() -> "Ensured index exists: " + indexName);
            }
        }

        for (String uniqueCol : metadata.getUnique()) {
            String indexName = "uk_" + metadata.getTableName() + "_" + uniqueCol;
            String createIdxSql = "CREATE UNIQUE INDEX IF NOT EXISTS " + indexName
                    + " ON " + metadata.getTableName() + " (" + uniqueCol + ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createIdxSql);
                logger.info(() -> "Ensured unique index exists: " + indexName);
            }
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
            return rs.next();
        }
    }
}
