package io.github.t1willi.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import io.github.t1willi.database.exception.DatabaseErrorType;
import io.github.t1willi.database.exception.DatabaseException;
import io.github.t1willi.database.exception.DatabaseExceptionMapper;

/**
 * Provides functionality for managing database-level metadata operations.
 * This includes database variables, triggers, procedures, functions and views.
 */
final class DatabaseMetadata {
    private static final Logger logger = Logger.getLogger(DatabaseMetadata.class.getName());

    // Prevent instantiation
    private DatabaseMetadata() {
    }

    /**
     * Creates or replaces a database variable using a key-value approach.
     * 
     * @param key   The variable name
     * @param value The variable value
     * @return true if the operation was successful
     */
    public static boolean injectVariable(String key, String value) {
        validateIdentifier(key);
        String sql = "DO $$\n" +
                "BEGIN\n" +
                "    PERFORM set_config('app." + key + "', ?, false);\n" +
                "EXCEPTION WHEN undefined_function THEN\n" +
                "    RAISE NOTICE 'First creating app namespace variables';\n" +
                "    PERFORM set_config('app." + key + "', ?, false);\n" +
                "END $$;";

        return executeStatement(sql, value, value);
    }

    /**
     * Retrieves a database variable value.
     * 
     * @param key The variable name
     * @return The variable value or null if not found
     */
    public static String getVariable(String key) {
        validateIdentifier(key);
        String sql = "SELECT current_setting('app." + key + "', true) AS value";

        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getString("value");
            }
            return null;
        } catch (SQLException e) {
            DatabaseException dbException = DatabaseExceptionMapper.map(e, sql, "database_variables");
            logger.severe(() -> dbException.getTechnicalDetails());
            throw dbException;
        }
    }

    /**
     * Retrieves all database variables with the app namespace.
     * 
     * @return Map of variable names to values
     */
    public static Map<String, String> getAllVariables() {
        String sql = "SELECT name, setting FROM pg_settings WHERE name LIKE 'app.%'";
        Map<String, String> variables = new HashMap<>();

        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String fullName = rs.getString("name");
                String name = fullName.substring(4); // Remove 'app.' prefix
                String value = rs.getString("setting");
                variables.put(name, value);
            }
            return variables;
        } catch (SQLException e) {
            DatabaseException dbException = DatabaseExceptionMapper.map(e, sql, "database_variables");
            logger.severe(() -> dbException.getTechnicalDetails());
            throw dbException;
        }
    }

    /**
     * Internal helper method to execute SQL statements.
     */
    private static boolean executeStatement(String sql, Object... params) {
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            ps.execute();
            logger.fine(() -> "Successfully executed database metadata operation: " + sql);
            return true;
        } catch (SQLException e) {
            DatabaseException dbException = DatabaseExceptionMapper.map(e, sql, "database_metadata");
            logger.severe(() -> dbException.getTechnicalDetails());
            throw dbException;
        }
    }

    /**
     * Validates that an identifier doesn't contain SQL injection attempts.
     */
    private static void validateIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Database identifier cannot be null or empty");
        }

        if (identifier.contains(";") || identifier.contains("--") ||
                identifier.contains("/*") || identifier.contains("*/") ||
                identifier.contains("'") || identifier.contains("\"") ||
                identifier.toLowerCase().contains("drop ") ||
                identifier.toLowerCase().contains("delete ")) {
            throw new DatabaseException(
                    DatabaseErrorType.DATA_INTEGRITY_ERROR,
                    "Invalid database identifier: may contain SQL injection attempt",
                    "The identifier '" + identifier + "' contains characters that could be used for SQL injection",
                    null);
        }

        if (DatabaseUtils.isReservedKeyword(identifier)) {
            logger.warning(() -> "Using a reserved keyword as identifier: " + identifier);
        }
    }
}