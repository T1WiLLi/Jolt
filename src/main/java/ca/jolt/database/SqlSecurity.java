package ca.jolt.database;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class SqlSecurity {
    // Commonly used SQL keywords for validation
    private static final Set<String> SQL_KEYWORDS = new HashSet<>();
    private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern VALID_COLUMN_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?$");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b)(union|select|insert|update|delete|drop|alter|exec|execute|create|where|having|or|and)(\\b)");
    private static final Pattern SAFE_WHERE_CLAUSE_PATTERN = Pattern.compile(
            "^(WHERE\\s+)?(([a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?)\\s*(=|!=|<>|>|<|>=|<=|LIKE|IN|IS NULL|IS NOT NULL|BETWEEN)\\s*((\\?)|('[^']*')|([0-9]+)))"
                    +
                    "(\\s+(AND|OR)\\s+([a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?)\\s*(=|!=|<>|>|<|>=|<=|LIKE|IN|IS NULL|IS NOT NULL|BETWEEN)\\s*((\\?)|('[^']*')|([0-9]+)))*$");

    static {
        String[] keywords = {
                "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
                "TABLE", "DATABASE", "UNION", "ALL", "JOIN", "INNER", "OUTER", "LEFT", "RIGHT",
                "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "SET", "VALUES", "INTO",
                "EXEC", "EXECUTE", "SP_", "XP_", "WAITFOR", "DELAY", "SHUTDOWN"
        };
        for (String keyword : keywords) {
            SQL_KEYWORDS.add(keyword.toUpperCase());
        }
    }

    /**
     * Validates a SQL identifier (table or column name)
     * 
     * @param identifier The identifier to validate
     * @return true if the identifier is valid
     */
    public static boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // Check if it matches allowed pattern and isn't a reserved keyword
        return VALID_IDENTIFIER_PATTERN.matcher(identifier).matches() &&
                !SQL_KEYWORDS.contains(identifier.toUpperCase());
    }

    /**
     * Validates a column name, which may include a table qualifier (table.column)
     * 
     * @param column The column name to validate
     * @return true if the column name is valid
     */
    public static boolean isValidColumnName(String column) {
        if (column == null || column.isEmpty()) {
            return false;
        }

        return VALID_COLUMN_PATTERN.matcher(column).matches() &&
                !containsSqlKeywords(column);
    }

    /**
     * Validates raw SQL to prevent potential injection attacks
     * 
     * @param sql The SQL string to validate
     * @return true if the SQL appears safe
     */
    public static boolean isValidRawSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }

        // Check for potentially dangerous patterns
        String normalized = sql.toLowerCase();
        return !normalized.contains("--") &&
                !normalized.contains(";") &&
                !normalized.contains("/*") &&
                !normalized.contains("*/") &&
                !normalized.contains("xp_") &&
                !normalized.contains("sp_") &&
                !normalized.contains("waitfor") &&
                !normalized.contains("delay") &&
                !checkForUnionBasedInjection(normalized) &&
                validateBalancedQuotes(sql);
    }

    /**
     * Validates a WHERE clause for potential SQL injection
     * 
     * @param whereClause The WHERE clause to validate
     * @return true if the WHERE clause appears safe
     */
    public static boolean isValidWhereClause(String whereClause) {
        if (whereClause == null || whereClause.isEmpty()) {
            return true;
        }

        if (SQL_INJECTION_PATTERN.matcher(whereClause).find()
                || SAFE_WHERE_CLAUSE_PATTERN.matcher(whereClause).find()) {
            return false;
        }

        return !whereClause.contains(";") &&
                !whereClause.contains("--") &&
                validateBalancedQuotes(whereClause) &&
                validateBalancedParentheses(whereClause);
    }

    /**
     * Sanitizes a SQL identifier by escaping special characters
     * Safe approach is to reject invalid identifiers rather than attempt to
     * sanitize
     * 
     * @param identifier The identifier to sanitize
     * @return The sanitized identifier or throws exception if invalid
     */
    public static String sanitizeIdentifier(String identifier) {
        if (!isValidIdentifier(identifier)) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return identifier;
    }

    /**
     * Ensures that a WHERE clause is safe for use in dynamic SQL
     * 
     * @param whereClause The WHERE clause to validate
     * @return The validated WHERE clause or throws exception if unsafe
     */
    public static String validateWhereClause(String whereClause) {
        if (!isValidWhereClause(whereClause)) {
            throw new IllegalArgumentException("Potentially unsafe WHERE clause: " + whereClause);
        }
        return whereClause;
    }

    /**
     * Checks for balanced quotes in a SQL string
     * 
     * @param sql The SQL string to check
     * @return true if quotes are balanced
     */
    private static boolean validateBalancedQuotes(String sql) {
        int singleQuotes = 0;
        int doubleQuotes = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                if (i > 0 && sql.charAt(i - 1) == '\\') {
                    continue;
                }
                singleQuotes++;
            } else if (c == '"') {
                if (i > 0 && sql.charAt(i - 1) == '\\') {
                    continue;
                }
                doubleQuotes++;
            }
        }

        return singleQuotes % 2 == 0 && doubleQuotes % 2 == 0;
    }

    /**
     * Checks for balanced parentheses in a SQL string
     * 
     * @param sql The SQL string to check
     * @return true if parentheses are balanced
     */
    private static boolean validateBalancedParentheses(String sql) {
        int count = 0;

        for (char c : sql.toCharArray()) {
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count < 0) {
                    return false;
                }
            }
        }

        return count == 0;
    }

    /**
     * Checks if a string contains SQL keywords
     * 
     * @param input The string to check
     * @return true if the string contains SQL keywords
     */
    private static boolean containsSqlKeywords(String input) {
        String upper = input.toUpperCase();
        for (String keyword : SQL_KEYWORDS) {
            if (upper.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for UNION-based SQL injection attempts
     * 
     * @param sql The SQL string to check
     * @return true if a potential UNION injection is detected
     */
    private static boolean checkForUnionBasedInjection(String sql) {
        return sql.matches(".*\\bunion\\b.*\\bselect\\b.*");
    }
}
