package ca.jolt.database.schema;

import ca.jolt.database.DatabaseUtils;
import ca.jolt.database.annotation.Check;
import ca.jolt.database.annotation.CheckEnum;
import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.ManyToOne;
import ca.jolt.database.annotation.Table;
import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;
import ca.jolt.database.exception.DatabaseExceptionMapper;
import ca.jolt.database.models.CheckConditionRegistry;
import ca.jolt.database.models.TableMetadata;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods shared by SchemaCreator, SchemaUpdater, etc.
 */
public final class SchemaUtils {
    private static final Logger logger = Logger.getLogger(SchemaUtils.class.getName());

    private SchemaUtils() {
    }

    public static void executeStatement(Connection conn, String sql,
            String successMessage, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info(() -> successMessage);
        } catch (SQLException e) {
            throw createDatabaseException(e, sql, tableName);
        }
    }

    private static DatabaseException createDatabaseException(SQLException e, String operation, String tableName) {
        DatabaseException dbException = DatabaseExceptionMapper.map(e, operation, tableName);
        logger.severe(() -> dbException.getTechnicalDetails());
        return dbException;
    }

    public static <T> String buildColumnDefinition(Field field, TableMetadata<T> metadata) {
        Column columnAnno = field.getAnnotation(Column.class);
        String columnName = metadata.getFieldToColumn().get(field.getName());

        String sqlType = determineSqlType(field, columnAnno);
        sqlType = addCheckConstraints(field, sqlType, columnName, metadata.getTableName());

        StringBuilder sb = new StringBuilder();
        sb.append(columnName).append(" ").append(sqlType);

        if (columnAnno != null && !columnAnno.nullable()) {
            sb.append(" NOT NULL");
        }

        if (field.isAnnotationPresent(Id.class)) {
            sb.append(" PRIMARY KEY");
        }

        return sb.toString();
    }

    private static String determineSqlType(Field field, Column columnAnno) {
        if (field.isAnnotationPresent(ManyToOne.class)) {
            return DatabaseUtils.getSqlTypeForForeignKey(field.getType());
        } else {
            return DatabaseUtils.getSqlTypeForJavaType(field.getType(), columnAnno);
        }
    }

    public static String addCheckConstraints(Field field, String sqlType,
            String columnName, String tableName) {
        CheckEnum enumAnno = field.getAnnotation(CheckEnum.class);
        if (enumAnno != null) {
            String[] allowedValues = enumAnno.values();
            String constraintName = tableName + "_" + columnName + "_check";
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

            System.out.println("Adding check constraint: " + constraintName);
            String message = "Allowed values: " + String.join(", ", allowedValues);
            CheckConditionRegistry.register(constraintName, message);
        }

        Check checkAnno = field.getAnnotation(Check.class);
        if (checkAnno != null) {
            String condition = checkAnno.condition().replace("?", columnName);
            String constraintName = tableName + "_" + columnName + "_check";

            StringBuilder checkSql = new StringBuilder();
            checkSql.append(" CHECK (").append(condition).append(")");
            sqlType += " " + checkSql.toString();

            String userMessage = checkAnno.message();
            if (userMessage == null || userMessage.isEmpty()) {
                userMessage = "Constraint: " + condition;
            }
            CheckConditionRegistry.register(constraintName, userMessage);
        }

        return sqlType;
    }

    public static <T> void updateCheckConstraint(Connection conn,
            TableMetadata<T> metadata,
            Map<String, Set<String>> existingConstraints,
            Field field, String colName) throws SQLException {
        Check checkAnno = field.getAnnotation(Check.class);
        if (checkAnno == null) {
            return;
        }
        String constraintName = metadata.getTableName() + "_" + colName + "_custom_check";
        String condition = checkAnno.condition().replace("?", colName);

        if (constraintExists(existingConstraints, metadata.getTableName(), constraintName)) {
            String checkClause = getConstraintDefinition(conn, constraintName);
            if (!checkClause.contains(condition)) {
                dropConstraint(conn, metadata.getTableName(), constraintName);
                addCheckConstraint(conn, metadata.getTableName(), constraintName, condition);
            }
        } else {
            addCheckConstraint(conn, metadata.getTableName(), constraintName, condition);
        }
    }

    public static <T> void updateCheckEnumConstraint(Connection conn, TableMetadata<T> metadata,
            Map<String, Set<String>> existingConstraints, Field field, String colName) throws SQLException {
        CheckEnum enumAnno = field.getAnnotation(CheckEnum.class);
        if (enumAnno == null) {
            return;
        }
        String constraintName = metadata.getTableName() + "_" + colName + "_check";
        String[] allowedValues = enumAnno.values();

        if (constraintExists(existingConstraints, metadata.getTableName(), constraintName)) {
            String currentValues = CheckConditionRegistry.getCondition(constraintName);
            if (currentValues == null) {
                currentValues = "";
            } else {

                currentValues = currentValues.replace("Allowed values: ", "")
                        .replace(", ", ",");
            }

            String newValuesCsv = String.join(",", allowedValues);

            if (!currentValues.equalsIgnoreCase(newValuesCsv)) {
                dropConstraint(conn, metadata.getTableName(), constraintName);
                addEnumConstraint(conn, metadata.getTableName(), constraintName, colName, allowedValues);
            }
        } else {
            addEnumConstraint(conn, metadata.getTableName(), constraintName, colName, allowedValues);
        }
    }

    public static <T> void updateNullableConstraint(Connection conn,
            TableMetadata<T> metadata,
            Map<String, Boolean> existingNullableStatus,
            Field field, String colName) throws SQLException {
        Column columnAnno = field.getAnnotation(Column.class);
        if (columnAnno == null) {
            return;
        }
        boolean currentNullable = existingNullableStatus.getOrDefault(colName.toLowerCase(), true);
        boolean shouldBeNullable = columnAnno.nullable();

        if (currentNullable != shouldBeNullable) {
            String alterSql = String.format("ALTER TABLE %s ALTER COLUMN %s %s",
                    metadata.getTableName(),
                    colName,
                    shouldBeNullable ? "DROP NOT NULL" : "SET NOT NULL");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                logger.info("Updated nullable status for column " + colName +
                        " in table " + metadata.getTableName() + " to " + shouldBeNullable);
            } catch (SQLException e) {
                logger.warning("Could not update nullable status for column '" + colName + "': " + e.getMessage());
            }
        }
    }

    public static <T> void updateColumnType(Connection conn,
            TableMetadata<T> metadata,
            Map<String, String> existingColumnTypes,
            Field field, String colName) throws SQLException {
        String currentType = existingColumnTypes.getOrDefault(colName.toLowerCase(), "");
        String expectedType = determineSqlType(field, field.getAnnotation(Column.class));

        String baseCurrent = currentType.replaceAll("\\(.*\\)", "").trim();
        String baseExpected = expectedType.replaceAll("\\(.*\\)", "").trim();

        if (!baseCurrent.equalsIgnoreCase(baseExpected)) {
            String alterSql = String.format("ALTER TABLE %s ALTER COLUMN %s TYPE %s",
                    metadata.getTableName(), colName, expectedType);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                logger.info("Updated column type for " + colName +
                        " in table " + metadata.getTableName() +
                        " from " + currentType + " to " + expectedType);
            } catch (SQLException e) {
                logger.warning("Could not update column type for '" + colName + "': " + e.getMessage());
            }
        }
    }

    private static boolean constraintExists(Map<String, Set<String>> existingConstraints,
            String tableName, String constraintName) {
        return existingConstraints.getOrDefault(tableName, Collections.emptySet())
                .contains(constraintName.toLowerCase());
    }

    private static void addCheckConstraint(Connection conn, String tableName,
            String constraintName, String condition) throws SQLException {
        String alterSql = String.format("ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s)",
                tableName, constraintName, condition);
        executeStatement(conn, alterSql,
                "Added CHECK constraint " + constraintName + " to table " + tableName,
                tableName);
    }

    public static void addEnumConstraint(Connection conn, String tableName, String constraintName,
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

        executeStatement(conn, checkSql.toString(),
                "Added CHECK ENUM constraint " + constraintName + " to table " + tableName,
                tableName);

        String message = "Allowed values: " + String.join(", ", allowedValues);
        CheckConditionRegistry.register(constraintName, message);
    }

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
     * Reads the current check_clause of a constraint from the DB.
     */
    public static String getConstraintDefinition(Connection conn, String constraintName) throws SQLException {
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
     * Utility method to retrieve existing constraints from the DB.
     */
    public static Map<String, Set<String>> getExistingConstraints(Connection conn, String tableName)
            throws SQLException {
        Map<String, Set<String>> constraints = new HashMap<>();
        String sql = "SELECT constraint_name, constraint_type " +
                "FROM information_schema.table_constraints WHERE table_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String constraintName = rs.getString("constraint_name").toLowerCase();
                    constraints.computeIfAbsent(tableName, k -> new HashSet<>()).add(constraintName);
                }
            }
        }
        return constraints;
    }

    /**
     * Retrieve existing column types from the DB, e.g. numeric(...), varchar(...).
     */
    public static Map<String, String> getExistingColumnTypes(Connection conn, String tableName) throws SQLException {
        Map<String, String> columnTypes = new HashMap<>();
        String sql = "SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale " +
                "FROM information_schema.columns WHERE table_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name").toLowerCase();
                    String dataType = rs.getString("data_type");
                    if ("character varying".equals(dataType)) {
                        int maxLength = rs.getInt("character_maximum_length");
                        if (!rs.wasNull()) {
                            dataType = "varchar(" + maxLength + ")";
                        }
                    } else if ("numeric".equals(dataType)) {
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
     * Retrieve existing nullability for each column in a table.
     */
    public static Map<String, Boolean> getExistingNullableStatus(Connection conn, String tableName)
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
     * Retrieves a map of existing column names for a given table.
     */
    public static Map<String, Boolean> getExistingColumns(Connection conn, String tableName) throws SQLException {
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
     * Returns all column names defined in the entity.
     */
    public static <T> Set<String> getEntityColumnNames(TableMetadata<T> metadata) {
        Set<String> columns = new HashSet<>();
        for (String colName : metadata.getFieldToColumn().values()) {
            columns.add(colName.toLowerCase());
        }
        return columns;
    }

    /**
     * Helpers for foreign keys, table name resolution, etc.
     */
    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    public static String getTargetTableName(Class<?> targetEntity) {
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

    public static String getForeignKeyIdColumn(Class<?> targetClass) {
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

    public static void addForeignKeyConstraint(Connection conn, String sourceTable, String sourceColumn,
            String targetTable, String targetColumn) throws SQLException {
        if (!foreignKeyExists(conn, sourceTable, targetTable, sourceColumn)) {
            String constraintName = "fk_" + sourceTable + "_" + sourceColumn;
            String alterSql = String.format(
                    "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s(%s)",
                    sourceTable, constraintName, sourceColumn, targetTable, targetColumn);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                logger.info(() -> "Added foreign key constraint: " + constraintName);
            } catch (SQLException e) {
                logger.warning("Could not add foreign key constraint '" + constraintName + "': " + e.getMessage());
            }
        }
    }

    public static boolean foreignKeyExists(Connection conn,
            String sourceTable,
            String targetTable,
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

    public static void extractAndRegisterCheckValues(String constraintName, String checkClause, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(checkClause);
        if (m.find()) {
            String valuesList = m.group(1);
            valuesList = valuesList.replace("'::text", "")
                    .replace("'", "")
                    .replace(", ", ",");
            String message = "Allowed values: " + valuesList;
            CheckConditionRegistry.register(constraintName, message);
        }
    }

    public static String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
