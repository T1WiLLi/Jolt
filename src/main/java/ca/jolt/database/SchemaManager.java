package ca.jolt.database;

import ca.jolt.database.annotation.Check;
import ca.jolt.database.annotation.CheckEnum;
import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.GenerationType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.ManyToOne;
import ca.jolt.database.annotation.Table;
import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;
import ca.jolt.database.exception.DatabaseExceptionMapper;
import ca.jolt.database.models.CheckEnumConstraintRegistry;
import ca.jolt.database.models.TableMetadata;
import ca.jolt.database.models.TableMetadata.RelationMetadata;
import ca.jolt.database.models.TableMetadata.RelationType;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

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
            createRelationships(conn, metadata);
            loadAllConstraints(conn, metadata.getTableName());
        } catch (SQLException e) {
            DatabaseException dbException = DatabaseExceptionMapper.map(
                    e,
                    "Schema validation for table " + metadata.getTableName(),
                    metadata.getTableName());
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
     * Creates join tables for ManyToMany relationships and adds foreign key
     * constraints for all relationship types.
     */
    private static <T> void createRelationships(Connection conn, TableMetadata<T> metadata) throws SQLException {
        for (String joinTableName : metadata.getJoinTables()) {
            if (!tableExists(conn, joinTableName)) {
                createJoinTable(conn, metadata, joinTableName);
            }
        }

        for (Map.Entry<String, RelationMetadata> entry : metadata.getRelationships().entrySet()) {
            RelationMetadata rel = entry.getValue();
            if (!rel.getMappedBy().isEmpty()) {
                continue;
            }
            if (rel.getType() == RelationType.MANY_TO_ONE) {
                addForeignKeyConstraint(
                        conn,
                        metadata.getTableName(),
                        rel.getJoinColumnName(),
                        getTargetTableName(rel.getTargetEntity()),
                        getForeignKeyIdColumn(rel.getTargetEntity()));
            }
        }
    }

    /**
     * Creates the join table for a ManyToMany relationship (owner side).
     */
    private static <T> void createJoinTable(Connection conn, TableMetadata<T> metadata, String joinTableName)
            throws SQLException {
        RelationMetadata rel = null;
        for (RelationMetadata r : metadata.getRelationships().values()) {
            if (r.getType() == RelationType.MANY_TO_MANY && joinTableName.equals(r.getJoinTable())) {
                if (r.getMappedBy().isEmpty()) {
                    rel = r;
                    break;
                }
            }
        }
        if (rel == null) {
            logger.warning("Could not find ManyToMany metadata for join table: " + joinTableName);
            return;
        }

        String joinColumn = rel.getJoinColumn();
        String inverseJoinColumn = rel.getInverseJoinColumn();

        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(joinTableName).append(" (");
        createSql.append(joinColumn).append(" INTEGER NOT NULL, ");
        createSql.append(inverseJoinColumn).append(" INTEGER NOT NULL, ");
        createSql.append("PRIMARY KEY (").append(joinColumn).append(", ").append(inverseJoinColumn).append("), ");

        createSql.append("CONSTRAINT fk_").append(joinTableName).append("_").append(joinColumn);
        createSql.append(" FOREIGN KEY (").append(joinColumn).append(") REFERENCES ");
        createSql.append(metadata.getTableName()).append("(").append(metadata.getIdColumn())
                .append(") ON DELETE CASCADE, ");

        createSql.append("CONSTRAINT fk_").append(joinTableName).append("_").append(inverseJoinColumn);
        createSql.append(" FOREIGN KEY (").append(inverseJoinColumn).append(") REFERENCES ");
        createSql.append(toSnakeCase(rel.getTargetEntity().getSimpleName())).append("(");
        createSql.append(getForeignKeyIdColumn(rel.getTargetEntity())).append(") ON DELETE CASCADE");

        createSql.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSql.toString());
            logger.info(() -> "Created join table: " + joinTableName);
        } catch (SQLException e) {
            DatabaseException dbException = DatabaseExceptionMapper.map(e, createSql.toString(), joinTableName);
            logger.severe(() -> dbException.getTechnicalDetails());
            throw dbException;
        }
    }

    /**
     * Builds the DDL for a single column, including type, primary key, identity,
     * etc.
     */
    private static <T> String buildColumnDefinition(Field field, TableMetadata<T> metadata) {
        Column columnAnno = field.getAnnotation(Column.class);
        String columnName = metadata.getFieldToColumn().get(field.getName());

        String sqlType;
        if (field.isAnnotationPresent(ManyToOne.class)) {
            sqlType = DatabaseUtils.getSqlTypeForForeignKey(field.getType());
        } else {
            sqlType = DatabaseUtils.getSqlTypeForJavaType(field.getType(), columnAnno);
        }

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

        Check checkAnno = field.getAnnotation(Check.class);
        if (checkAnno != null) {
            String condition = checkAnno.condition().replace("?", columnName);
            StringBuilder checkSql = new StringBuilder();
            checkSql.append(" CHECK (").append(condition).append(")");
            sqlType += " " + checkSql.toString();
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

    private static <T> void updateTableSchema(Connection conn, TableMetadata<T> metadata) throws SQLException {
        Map<String, Boolean> existingColumns = getExistingColumns(conn, metadata.getTableName());
        Set<String> entityColumns = getEntityColumnNames(metadata);

        addMissingColumns(conn, metadata, existingColumns);
        removeExtraColumns(conn, metadata, existingColumns, entityColumns);
        updateConstraint(conn, metadata, existingColumns);
    }

    /**
     * Updates constraints on existing columns to match entity annotations.
     */
    private static <T> void updateConstraint(Connection conn, TableMetadata<T> metadata,
            Map<String, Boolean> existingColumns) throws SQLException {
        Map<String, Set<String>> existingConstraints = getExistingConstraints(conn, metadata.getTableName());
        Map<String, String> existingColumnTypes = getExistingColumnTypes(conn, metadata.getTableName());
        Map<String, Boolean> existingNullableStatus = getExistingNullableStatus(conn, metadata.getTableName());

        for (Field field : metadata.getEntityClass().getDeclaredFields()) {
            String colName = metadata.getFieldToColumn().get(field.getName());
            if (colName == null || !existingColumns.containsKey(colName.toLowerCase())) {
                continue;
            }

            updateCheckConstraint(conn, metadata, existingConstraints, field, colName);
            updateCheckEnumConstraint(conn, metadata, existingConstraints, field, colName);
            updateNullableConstraint(conn, metadata, existingNullableStatus, field, colName);
            updateColumnType(conn, metadata, existingColumnTypes, field, colName);
        }
    }

    /**
     * Updates custom CHECK constraints from @Check annotation.
     */
    private static <T> void updateCheckConstraint(Connection conn, TableMetadata<T> metadata,
            Map<String, Set<String>> existingConstraints, Field field, String colName) throws SQLException {
        Check checkAnno = field.getAnnotation(Check.class);
        if (checkAnno != null) {
            String constraintName = metadata.getTableName() + "_" + colName + "_custom_check";

            if (existingConstraints.getOrDefault(metadata.getTableName(), Collections.emptySet())
                    .contains(constraintName.toLowerCase())) {
                String condition = checkAnno.condition().replace("?", colName);
                String checkClause = getConstraintDefinition(conn, constraintName);

                if (!checkClause.contains(condition)) {
                    dropConstraint(conn, metadata.getTableName(), constraintName);
                    addCheckConstraint(conn, metadata.getTableName(), constraintName, condition);
                }
            } else {
                String condition = checkAnno.condition().replace("?", colName);
                addCheckConstraint(conn, metadata.getTableName(), constraintName, condition);
            }
        }
    }

    /**
     * Updates CHECK constraints from @CheckEnum annotation.
     */
    private static <T> void updateCheckEnumConstraint(Connection conn, TableMetadata<T> metadata,
            Map<String, Set<String>> existingConstraints, Field field, String colName) throws SQLException {
        CheckEnum enumAnno = field.getAnnotation(CheckEnum.class);
        if (enumAnno != null) {
            String constraintName = metadata.getTableName() + "_" + colName + "_check";

            if (existingConstraints.getOrDefault(metadata.getTableName(), Collections.emptySet())
                    .contains(constraintName.toLowerCase())) {
                String[] allowedValues = enumAnno.values();
                String currentValues = CheckEnumConstraintRegistry.getAllowedValues(constraintName);

                if (currentValues == null || !buildEnumValuesString(allowedValues).equals(currentValues)) {
                    dropConstraint(conn, metadata.getTableName(), constraintName);
                    addEnumConstraint(conn, metadata.getTableName(), constraintName, colName, allowedValues);
                }
            } else {
                addEnumConstraint(conn, metadata.getTableName(), constraintName, colName, enumAnno.values());
            }
        }
    }

    /**
     * Updates NOT NULL constraints based on @Column(nullable) annotation.
     */
    private static <T> void updateNullableConstraint(Connection conn, TableMetadata<T> metadata,
            Map<String, Boolean> existingNullableStatus, Field field, String colName) throws SQLException {
        Column columnAnno = field.getAnnotation(Column.class);
        if (columnAnno != null) {
            boolean currentNullable = existingNullableStatus.getOrDefault(colName.toLowerCase(), true);
            boolean shouldBeNullable = columnAnno.nullable();

            if (currentNullable != shouldBeNullable) {
                String alterSql;
                if (shouldBeNullable) {
                    alterSql = "ALTER TABLE " + metadata.getTableName() +
                            " ALTER COLUMN " + colName + " DROP NOT NULL";
                } else {
                    alterSql = "ALTER TABLE " + metadata.getTableName() +
                            " ALTER COLUMN " + colName + " SET NOT NULL";
                }

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterSql);
                    logger.info("Updated nullable status for column " + colName +
                            " in table " + metadata.getTableName() + " to " + shouldBeNullable);
                } catch (SQLException e) {
                    logger.warning("Could not update nullable status for column '" + colName + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Updates column type if it changed in the entity definition.
     */
    private static <T> void updateColumnType(Connection conn, TableMetadata<T> metadata,
            Map<String, String> existingColumnTypes, Field field, String colName) throws SQLException {
        Column columnAnno = field.getAnnotation(Column.class);
        String currentType = existingColumnTypes.getOrDefault(colName.toLowerCase(), "");

        String expectedType;
        if (field.isAnnotationPresent(ManyToOne.class)) {
            expectedType = DatabaseUtils.getSqlTypeForForeignKey(field.getType());
        } else {
            expectedType = DatabaseUtils.getSqlTypeForJavaType(field.getType(), columnAnno);
        }

        String baseCurrentType = currentType.replaceAll("\\(.*\\)", "").trim();
        String baseExpectedType = expectedType.replaceAll("\\(.*\\)", "").trim();

        if (!baseCurrentType.equalsIgnoreCase(baseExpectedType)) {
            String alterSql = "ALTER TABLE " + metadata.getTableName() +
                    " ALTER COLUMN " + colName + " TYPE " + expectedType;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                logger.info("Updated column type for " + colName +
                        " in table " + metadata.getTableName() + " from " + currentType + " to " + expectedType);
            } catch (SQLException e) {
                logger.warning("Could not update column type for '" + colName + "': " + e.getMessage());
            }
        }
    }

    /**
     * Adds a CHECK constraint for custom conditions.
     */
    private static void addCheckConstraint(Connection conn, String tableName, String constraintName,
            String condition) throws SQLException {
        String alterSql = "ALTER TABLE " + tableName +
                " ADD CONSTRAINT " + constraintName +
                " CHECK (" + condition + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(alterSql);
            logger.info("Added CHECK constraint " + constraintName + " to table " + tableName);
        } catch (SQLException e) {
            logger.warning("Could not add CHECK constraint '" + constraintName + "': " + e.getMessage());
        }
    }

    /**
     * Adds a CHECK constraint for enum values.
     */
    private static void addEnumConstraint(Connection conn, String tableName, String constraintName,
            String columnName, String[] allowedValues) throws SQLException {
        StringBuilder checkSql = new StringBuilder();
        checkSql.append("ALTER TABLE ").append(tableName);
        checkSql.append(" ADD CONSTRAINT ").append(constraintName);
        checkSql.append(" CHECK (").append(columnName).append(" IN (");

        for (int i = 0; i < allowedValues.length; i++) {
            checkSql.append("'").append(allowedValues[i]).append("'");
            if (i < allowedValues.length - 1) {
                checkSql.append(", ");
            }
        }
        checkSql.append("))");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(checkSql.toString());
            logger.info("Added CHECK ENUM constraint " + constraintName + " to table " + tableName);
            CheckEnumConstraintRegistry.register(constraintName, buildEnumValuesString(allowedValues));
        } catch (SQLException e) {
            logger.warning("Could not add CHECK ENUM constraint '" + constraintName + "': " + e.getMessage());
        }
    }

    /**
     * Drops a constraint by name.
     */
    private static void dropConstraint(Connection conn, String tableName, String constraintName) throws SQLException {
        String dropSql = "ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + constraintName;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(dropSql);
            logger.info("Dropped constraint " + constraintName + " from table " + tableName);
        } catch (SQLException e) {
            logger.warning("Could not drop constraint '" + constraintName + "': " + e.getMessage());
        }
    }

    /**
     * Builds a string representation of enum values for comparison.
     */
    private static String buildEnumValuesString(String[] values) {
        return String.join(",", values);
    }

    /**
     * Gets the definition of a constraint from the database.
     */
    private static String getConstraintDefinition(Connection conn, String constraintName) throws SQLException {
        String sql = "SELECT check_clause FROM information_schema.check_constraints " +
                "WHERE constraint_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, constraintName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("check_clause");
                }
            }
        }
        return "";
    }

    /**
     * Gets existing constraints for a table.
     */
    private static Map<String, Set<String>> getExistingConstraints(Connection conn, String tableName)
            throws SQLException {
        Map<String, Set<String>> constraints = new HashMap<>();

        String sql = "SELECT constraint_name, constraint_type FROM information_schema.table_constraints " +
                "WHERE table_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String constraintName = rs.getString("constraint_name").toLowerCase();
                    constraints.computeIfAbsent(tableName, k -> new HashSet<>())
                            .add(constraintName);
                }
            }
        }

        return constraints;
    }

    /**
     * Gets existing column types for a table.
     */
    private static Map<String, String> getExistingColumnTypes(Connection conn, String tableName) throws SQLException {
        Map<String, String> columnTypes = new HashMap<>();

        String sql = "SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale " +
                "FROM information_schema.columns WHERE table_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name").toLowerCase();
                    String dataType = rs.getString("data_type");

                    // Add size information for certain types
                    if (dataType.equals("character varying")) {
                        int maxLength = rs.getInt("character_maximum_length");
                        if (!rs.wasNull()) {
                            dataType = "varchar(" + maxLength + ")";
                        }
                    } else if (dataType.equals("numeric")) {
                        int precision = rs.getInt("numeric_precision");
                        int scale = rs.getInt("numeric_scale");
                        if (!rs.wasNull()) {
                            dataType = "numeric(" + precision + "," + scale + ")";
                        }
                    }

                    columnTypes.put(columnName, dataType);
                }
            }
        }

        return columnTypes;
    }

    /**
     * Gets existing nullable status for columns in a table.
     */
    private static Map<String, Boolean> getExistingNullableStatus(Connection conn, String tableName)
            throws SQLException {
        Map<String, Boolean> nullableStatus = new HashMap<>();

        String sql = "SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name").toLowerCase();
                    boolean isNullable = "YES".equalsIgnoreCase(rs.getString("is_nullable"));
                    nullableStatus.put(columnName, isNullable);
                }
            }
        }

        return nullableStatus;
    }

    /**
     * Gets existing columns for a table.
     */
    private static Map<String, Boolean> getExistingColumns(Connection conn, String tableName) throws SQLException {
        Map<String, Boolean> existingColumns = new HashMap<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(null, null, tableName.toLowerCase(), null)) {
            while (rs.next()) {
                existingColumns.put(rs.getString("COLUMN_NAME").toLowerCase(), true);
            }
        }

        return existingColumns;
    }

    /**
     * Gets all column names defined in the entity.
     */
    private static <T> Set<String> getEntityColumnNames(TableMetadata<T> metadata) {
        Set<String> columns = new HashSet<>();
        for (String colName : metadata.getFieldToColumn().values()) {
            columns.add(colName.toLowerCase());
        }
        return columns;
    }

    /**
     * Adds missing columns to the table.
     */
    private static <T> void addMissingColumns(Connection conn, TableMetadata<T> metadata,
            Map<String, Boolean> existingColumns) throws SQLException {
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
     * Removes columns that are not in the entity definition.
     */
    private static <T> void removeExtraColumns(Connection conn, TableMetadata<T> metadata,
            Map<String, Boolean> existingColumns, Set<String> entityColumns) throws SQLException {
        for (String colName : existingColumns.keySet()) {
            if (!entityColumns.contains(colName.toLowerCase())
                    && !colName.equals(metadata.getIdColumn().toLowerCase())) {
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

    /**
     * Creates indexes for columns specified in @Table(indexes={...}) and also for
     * foreign key columns.
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

        for (RelationMetadata rel : metadata.getRelationships().values()) {
            if (rel.getType() == RelationType.MANY_TO_ONE && rel.getMappedBy().isEmpty()) {
                String colName = rel.getJoinColumnName();
                String indexName = "idx_" + metadata.getTableName() + "_" + colName;
                String createIdxSql = "CREATE INDEX IF NOT EXISTS " + indexName
                        + " ON " + metadata.getTableName() + " (" + colName + ")";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createIdxSql);
                } catch (SQLException e) {
                    logger.warning(
                            "Could not create index for foreign key column '" + colName + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Loads check constraints from the database (e.g., those created
     * for @CheckEnum).
     */
    public static <T> void loadAllConstraints(Connection conn, String tableName) {
        try {
            String sql = "SELECT constraint_name, check_clause FROM information_schema.check_constraints " +
                    "JOIN information_schema.constraint_column_usage USING (constraint_name) " +
                    "WHERE constraint_name LIKE ? AND constraint_name LIKE '%_check'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, tableName + "_%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String constraintName = rs.getString("constraint_name");
                        String checkClause = rs.getString("check_clause");
                        if (checkClause.contains("ANY (ARRAY[")) {
                            int startIdx = checkClause.indexOf("ARRAY[") + 6;
                            int endIdx = checkClause.lastIndexOf("]");
                            if (startIdx > 0 && endIdx > startIdx) {
                                String valuesList = checkClause.substring(startIdx, endIdx);
                                valuesList = valuesList.replace("'::text", "").replace("'", "").replace(", ", ",");
                                CheckEnumConstraintRegistry.register(constraintName, valuesList);
                            }
                        } else if (checkClause.contains(" IN (")) {
                            int startIdx = checkClause.indexOf("(") + 1;
                            int endIdx = checkClause.lastIndexOf(")");
                            if (startIdx > 0 && endIdx > startIdx) {
                                String valuesList = checkClause.substring(startIdx, endIdx);
                                valuesList = valuesList.replace("'", "").replace(", ", ",");
                                CheckEnumConstraintRegistry.register(constraintName, valuesList);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load check constraints for table " + tableName + ": " + e.getMessage());
        }
    }

    /**
     * Checks if a table exists (case-insensitive).
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    /**
     * Gets the table name for a target entity by reading its @Table annotation or
     * falling back to snake_case.
     * Also checks for reserved keywords.
     */
    private static String getTargetTableName(Class<?> targetEntity) {
        Table tableAnno = targetEntity.getAnnotation(Table.class);
        String tableName = (tableAnno != null && !tableAnno.table().isEmpty())
                ? tableAnno.table()
                : toSnakeCase(targetEntity.getSimpleName());
        if (DatabaseUtils.isReservedKeyword(tableName)) {
            logger.severe(() -> "Table name " + tableName
                    + " is a reserved keyword. Please rename the table using @Table annotation.");
            throw new DatabaseException(
                    DatabaseErrorType.CONSTRAINT_VIOLATION,
                    "Table name '" + tableName + "' for entity " + targetEntity.getSimpleName() +
                            " is a reserved word. Please change the @Table annotation value to a non-reserved name.",
                    null,
                    null);
        }
        return tableName;
    }

    /**
     * Looks up the ID column for a target entity. If no @Column is specified,
     * defaults to snake_case of the field name.
     */
    private static String getForeignKeyIdColumn(Class<?> targetClass) {
        try {
            for (Field field : targetClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    Column columnAnno = field.getAnnotation(Column.class);
                    if (columnAnno != null && !columnAnno.value().isEmpty()) {
                        return columnAnno.value();
                    }
                    return toSnakeCase(field.getName());
                }
            }
        } catch (Exception e) {
            logger.warning("Could not determine ID column for class " + targetClass.getName());
        }
        return "id";
    }

    /**
     * Adds a foreign key constraint to the source table if it does not already
     * exist.
     */
    private static void addForeignKeyConstraint(Connection conn, String sourceTable, String sourceColumn,
            String targetTable, String targetColumn) throws SQLException {
        if (!foreignKeyExists(conn, sourceTable, targetTable, sourceColumn)) {
            String constraintName = "fk_" + sourceTable + "_" + sourceColumn;
            String alterSql = "ALTER TABLE " + sourceTable +
                    " ADD CONSTRAINT " + constraintName +
                    " FOREIGN KEY (" + sourceColumn + ") REFERENCES " +
                    targetTable + "(" + targetColumn + ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                logger.info(() -> "Added foreign key constraint: " + constraintName);
            } catch (SQLException e) {
                logger.warning("Could not add foreign key constraint '" + constraintName + "': " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a foreign key constraint already exists.
     */
    private static boolean foreignKeyExists(Connection conn, String sourceTable, String targetTable,
            String sourceColumn) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getImportedKeys(null, null, sourceTable)) {
            while (rs.next()) {
                String pkTable = rs.getString("PKTABLE_NAME");
                String fkColumn = rs.getString("FKCOLUMN_NAME");
                if (pkTable.equalsIgnoreCase(targetTable) && fkColumn.equalsIgnoreCase(sourceColumn)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts a Java class name into snake_case.
     */
    private static String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
