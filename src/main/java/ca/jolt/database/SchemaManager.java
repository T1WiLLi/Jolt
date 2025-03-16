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

import ca.jolt.database.annotation.CheckEnum;
import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.GenerationType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.exception.DatabaseException;
import ca.jolt.database.exception.DatabaseExceptionMapper;
import ca.jolt.database.models.CheckEnumConstraintRegistry;
import ca.jolt.database.models.TableMetadata;

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
            DatabaseException dbException = DatabaseExceptionMapper.map(e,
                    "Schema validation for table " + metadata.getTableName(), metadata.getTableName());
            logger.severe(() -> dbException.getTechnicalDetails());
            throw dbException;
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
            } catch (SQLException e) {
                DatabaseException dbException = DatabaseExceptionMapper.map(e, seqSql, metadata.getTableName());
                logger.severe(() -> dbException.getTechnicalDetails());
                throw dbException;
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSql.toString());
            logger.info(() -> "Created table: " + metadata.getTableName());
        } catch (SQLException e) {
            DatabaseException dbException = DatabaseExceptionMapper.map(e, createSql.toString(),
                    metadata.getTableName());
            logger.severe(() -> dbException.getTechnicalDetails());
            throw dbException;
        }
    }

    /**
     * Builds the DDL for a single column, including primary key and identity
     * settings.
     * For String types, if no positive length is provided via the @Column
     * annotation,
     * the SQL type will default to TEXT.
     */
    private static <T> String buildColumnDefinition(Field field, TableMetadata<T> metadata) {
        Column columnAnno = field.getAnnotation(Column.class);
        String columnName = metadata.getFieldToColumn().get(field.getName());

        String sqlType = DatabaseUtils.getSqlTypeForJavaType(field.getType(), columnAnno);

        CheckEnum enumAnno = field.getAnnotation(CheckEnum.class);
        if (enumAnno != null) {
            String[] allowedValues = enumAnno.values();
            String constraintName = metadata.getTableName() + "_" + columnName + "_check";
            StringBuilder checkSql = new StringBuilder();
            checkSql.append(" CHECK (").append(columnName).append(" IN (");
            for (int i = 0; i < allowedValues.length; i++) {
                checkSql.append("'").append(allowedValues[i]).append("'");
                if (i < allowedValues.length - 1) {
                    checkSql.append(", ");
                }
            }
            checkSql.append("))");
            sqlType += " " + checkSql.toString();
            CheckEnumConstraintRegistry.register(constraintName, String.join(", ", allowedValues));
        }

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
     * Checks the existing table schema and adds any missing columns.
     */
    private static <T> void updateTableSchema(Connection conn, TableMetadata<T> metadata) throws SQLException {
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
                } catch (SQLException e) {
                    DatabaseException dbException = DatabaseExceptionMapper.map(e, alterSql, metadata.getTableName());
                    logger.severe(() -> dbException.getTechnicalDetails());
                    throw dbException;
                }
            }
        }

    }

    /**
     * Manages indexes while avoiding duplicate index creation.
     */
    private static <T> void manageIndexes(Connection conn, TableMetadata<T> metadata) throws SQLException {
        for (String indexCol : metadata.getIndexes()) {
            if (metadata.getUnique().contains(indexCol) ||
                    indexCol.equals(metadata.getFieldToColumn().get(metadata.getId().getName()))) {
                continue;
            }
            String indexName = "idx_" + metadata.getTableName() + "_" + indexCol;
            String createIdxSql = "CREATE INDEX IF NOT EXISTS " + indexName
                    + " ON " + metadata.getTableName() + " (" + indexCol + ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createIdxSql);
            } catch (SQLException e) {
                DatabaseException dbException = DatabaseExceptionMapper.map(e, createIdxSql, metadata.getTableName());
                logger.severe(() -> dbException.getTechnicalDetails());
                throw dbException;
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
