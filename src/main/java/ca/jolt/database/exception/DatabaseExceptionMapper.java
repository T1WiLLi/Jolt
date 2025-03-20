package ca.jolt.database.exception;

import java.sql.SQLException;

public class DatabaseExceptionMapper {

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
        return new DatabaseException(
                DatabaseErrorType.UNKNOWN_ERROR,
                "An unexpected database error occurred. Please try again later.",
                String.format("Unhandled database error: %s, SQL State: %s, SQL: %s", errorMessage, sqlState,
                        sanitizeSql(sql)),
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

    private static String sanitizeSql(String sql) {
        if (sql == null) {
            return "null";
        }
        return sql.replaceAll("(?i)(password|secret|token|key)\\s*=\\s*'[^']*'", "$1='[REDACTED]'");
    }
}