package ca.jolt.database.exception;

import java.sql.SQLException;
import java.util.regex.Pattern;

import ca.jolt.database.models.CheckEnumConstraintRegistry;

public class DatabaseExceptionMapper {

    private static final Pattern UNIQUE_CONSTRAINT_PATTERN = Pattern
            .compile("Key \\((.+?)\\)=\\((.+?)\\) already exists");
    private static final Pattern CHECK_CONSTRAINT_NAME_PATTERN = Pattern
            .compile("violates check constraint");

    public static DatabaseException map(SQLException e, String sql, String entityName) {
        String sqlState = e.getSQLState();
        String errorMessage = e.getMessage();

        if (isConnectionError(e)) {
            return new DatabaseException(
                    DatabaseErrorType.CONNECTION_ERROR,
                    "Database service is temporarily unavailable. Please try again later.",
                    String.format("Connection error: %s, SQL: %s", errorMessage, sanitizeSql(sql)),
                    e);
        }

        if (sqlState != null && sqlState.startsWith("23")) {
            String lowerMsg = errorMessage.toLowerCase();
            if (lowerMsg.contains("duplicate")) {
                String field = extractConstraintField(errorMessage);
                return new DatabaseException(
                        DatabaseErrorType.DUPLICATE_KEY,
                        String.format("A %s with this %s already exists.", entityName, field),
                        String.format("Duplicate key violation: %s, SQL: %s", errorMessage, sanitizeSql(sql)),
                        e);
            }
            if (lowerMsg.contains("foreign key")) {
                return new DatabaseException(
                        DatabaseErrorType.CONSTRAINT_VIOLATION,
                        "This operation references data that doesn't exist or would create orphaned data.",
                        String.format("Foreign key violation: %s, SQL: %s", errorMessage, sanitizeSql(sql)),
                        e);
            }
            if (lowerMsg.contains("check constraint")) {
                var matcher = CHECK_CONSTRAINT_NAME_PATTERN.matcher(errorMessage);
                if (matcher.find()) {
                    String constraintName = matcher.group();
                    String allowedValues = CheckEnumConstraintRegistry.getAllowedValues(constraintName);
                    if (allowedValues != null) {
                        return new DatabaseException(
                                DatabaseErrorType.DATA_INTEGRITY_ERROR,
                                String.format(
                                        "Invalid value provided. Please adjust the field '%s' to one of the following: %s.",
                                        constraintName.replace("chk_" + entityName.toLowerCase() + "_", ""),
                                        allowedValues),
                                String.format("Check constraint violation: %s, SQL: %s", errorMessage,
                                        sanitizeSql(sql)),
                                e);
                    }
                }
                return new DatabaseException(
                        DatabaseErrorType.DATA_INTEGRITY_ERROR,
                        "The data provided doesn't meet the required constraints.",
                        String.format("Check constraint violation: %s, SQL: %s", errorMessage, sanitizeSql(sql)),
                        e);
            }
            if (lowerMsg.contains("not null")) {
                String columnName = extractNotNullColumn(errorMessage);
                return new DatabaseException(
                        DatabaseErrorType.DATA_INTEGRITY_ERROR,
                        String.format("The %s field is required.", columnName),
                        String.format("Not null violation: %s, SQL: %s", errorMessage, sanitizeSql(sql)),
                        e);
            }
        }

        return new DatabaseException(
                DatabaseErrorType.UNKNOWN_ERROR,
                "An unexpected database error occurred. Please try again later.",
                String.format("Unhandled database error: %s, SQL State: %s, SQL: %s", errorMessage, sqlState,
                        sanitizeSql(sql)),
                e);
    }

    public static DatabaseException mapMappingError(Exception e, Class<?> entityClass) {
        return new DatabaseException(
                DatabaseErrorType.MAPPING_ERROR,
                "Unable to process the data. Please check your inputs.",
                String.format("Error mapping result to %s: %s", entityClass.getName(), e.getMessage()),
                e);
    }

    private static boolean isConnectionError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("connection")
                && (message.contains("refused")
                        || message.contains("closed")
                        || message.contains("timeout")
                        || message.contains("terminated"));
    }

    private static String extractConstraintField(String message) {
        if (message == null) {
            return "field";
        }
        String constraintName = null;
        if (message.contains("constraint \"")) {
            int start = message.indexOf("constraint \"") + 12;
            int end = message.indexOf("\"", start);
            if (end > start) {
                constraintName = message.substring(start, end);
            }
        }
        if (constraintName == null) {
            var matcher = UNIQUE_CONSTRAINT_PATTERN.matcher(message);
            if (matcher.find()) {
                return matcher.group(1).replace("\"", "");
            }
            return "field";
        }
        return constraintName;
    }

    private static String extractNotNullColumn(String message) {
        if (message != null && message.contains("column \"")) {
            int start = message.indexOf("column \"") + 8;
            int end = message.indexOf("\"", start);
            if (end > start) {
                return message.substring(start, end);
            }
        }
        if (message != null && message.toLowerCase().contains("cannot be null")) {
            return "required";
        }
        return "field";
    }

    private static String sanitizeSql(String sql) {
        if (sql == null) {
            return "null";
        }
        return sql.replaceAll("(?i)(password|secret|token|key)\\s*=\\s*'[^']*'", "$1='[REDACTED]'");
    }
}
