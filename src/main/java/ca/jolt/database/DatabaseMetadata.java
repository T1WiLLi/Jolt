package ca.jolt.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ca.jolt.database.exception.DatabaseErrorType;
import ca.jolt.database.exception.DatabaseException;
import ca.jolt.database.exception.DatabaseExceptionMapper;

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
     * Creates or replaces a database trigger function.
     * 
     * @param name         The name of the trigger function
     * @param functionBody The SQL function body in PL/pgSQL
     * @param parameters   Map of parameter names to their SQL types
     * @param returnType   The SQL return type (default: trigger)
     * @return true if the operation was successful
     */
    public static boolean createTriggerFunction(String name, String functionBody,
            Map<String, String> parameters,
            String returnType) {
        validateIdentifier(name);
        if (returnType == null || returnType.isEmpty()) {
            returnType = "trigger";
        }

        StringBuilder paramStr = new StringBuilder();
        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach((paramName, paramType) -> {
                validateIdentifier(paramName);
                if (paramStr.length() > 0) {
                    paramStr.append(", ");
                }
                paramStr.append(paramName).append(" ").append(paramType);
            });
        }

        String sql = "CREATE OR REPLACE FUNCTION " + name + "(" + paramStr + ")\n" +
                "RETURNS " + returnType + " AS $$\n" +
                "BEGIN\n" +
                functionBody + "\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;";

        return executeStatement(sql);
    }

    /**
     * Creates or replaces a database trigger.
     * 
     * @param name            The name of the trigger
     * @param table           The table to attach the trigger to
     * @param triggerFunction The name of the trigger function to call
     * @param triggerTiming   When the trigger should fire (BEFORE, AFTER, INSTEAD
     *                        OF)
     * @param triggerEvents   Events that activate the trigger (INSERT, UPDATE,
     *                        DELETE)
     * @param forEachRow      True for ROW triggers, false for STATEMENT triggers
     * @return true if the operation was successful
     */
    public static boolean createTrigger(String name, String table, String triggerFunction,
            String triggerTiming, String[] triggerEvents,
            boolean forEachRow) {
        validateIdentifier(name);
        validateIdentifier(table);
        validateIdentifier(triggerFunction);

        if (triggerEvents == null || triggerEvents.length == 0) {
            throw new IllegalArgumentException("At least one trigger event must be specified");
        }

        StringBuilder events = new StringBuilder();
        for (int i = 0; i < triggerEvents.length; i++) {
            if (i > 0) {
                events.append(" OR ");
            }
            events.append(triggerEvents[i]);
        }

        String forEachClause = forEachRow ? "FOR EACH ROW" : "FOR EACH STATEMENT";

        String sql = "CREATE OR REPLACE TRIGGER " + name + "\n" +
                triggerTiming + " " + events + " ON " + table + "\n" +
                forEachClause + " EXECUTE FUNCTION " + triggerFunction + "();";

        return executeStatement(sql);
    }

    /**
     * Creates or replaces a database view.
     * 
     * @param name  The name of the view
     * @param query The SQL query that defines the view
     * @return true if the operation was successful
     */
    public static boolean createView(String name, String query) {
        validateIdentifier(name);

        String sql = "CREATE OR REPLACE VIEW " + name + " AS\n" + query;

        return executeStatement(sql);
    }

    /**
     * Creates or replaces a database function or procedure.
     * 
     * @param name         The name of the function
     * @param functionBody The SQL function body
     * @param parameters   Map of parameter names to their SQL types
     * @param returnType   The SQL return type (null for procedures)
     * @param language     The procedural language to use (default: plpgsql)
     * @param isStrict     Whether the function is STRICT (returns null for null
     *                     input)
     * @param volatility   VOLATILE, STABLE, or IMMUTABLE
     * @return true if the operation was successful
     */
    public static boolean createFunction(String name, String functionBody,
            Map<String, String> parameters,
            String returnType, String language,
            boolean isStrict, String volatility) {
        validateIdentifier(name);

        if (language == null || language.isEmpty()) {
            language = "plpgsql";
        }

        StringBuilder paramStr = new StringBuilder();
        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach((paramName, paramType) -> {
                validateIdentifier(paramName);
                if (paramStr.length() > 0) {
                    paramStr.append(", ");
                }
                paramStr.append(paramName).append(" ").append(paramType);
            });
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR REPLACE FUNCTION ").append(name).append("(").append(paramStr).append(")\n");

        if (returnType != null && !returnType.isEmpty()) {
            sql.append("RETURNS ").append(returnType).append("\n");
        }

        sql.append("AS $$\n")
                .append(functionBody).append("\n")
                .append("$$ LANGUAGE ").append(language);

        if (isStrict) {
            sql.append(" STRICT");
        }

        if (volatility != null && !volatility.isEmpty()) {
            sql.append(" ").append(volatility);
        }

        sql.append(";");

        return executeStatement(sql.toString());
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