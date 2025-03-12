package ca.jolt.database.core;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages SQL dialects and provides database-specific operations based on JDBC
 * URLs.
 * This class uses enum-based design for type safety and extensibility.
 */
public class SQLDialect {

    /**
     * Enumeration of supported database dialects with their specific properties and
     * behaviors.
     * Each dialect encapsulates its own configuration and SQL syntax rules.
     */
    public enum Dialect {
        MYSQL("jdbc:mysql:", "com.mysql.jdbc.Driver", "mysql",
                id -> "`" + id + "`",
                (limit, offset) -> "LIMIT " + limit + " OFFSET " + offset),

        POSTGRESQL("jdbc:postgresql:", "org.postgresql.Driver", "postgres",
                id -> "\"" + id + "\"",
                (limit, offset) -> "LIMIT " + limit + " OFFSET " + offset),

        ORACLE("jdbc:oracle:", "oracle.jdbc.driver.OracleDriver", "oracle",
                id -> "\"" + id + "\"",
                (limit, offset) -> "OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY"),

        SQLSERVER("jdbc:sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver",
                id -> "\"" + id + "\"",
                (limit, offset) -> "OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY"),

        H2("jdbc:h2:", "org.h2.Driver", "h2",
                id -> "\"" + id + "\"",
                (limit, offset) -> "LIMIT " + limit + " OFFSET " + offset),

        UNKNOWN("", "", "unknown",
                id -> id,
                (limit, offset) -> "");

        private final String jdbcPrefix;
        private final String driverClass;
        private final String dialectName;
        private final Function<String, String> identifierQuoter;
        private final PaginationStrategy paginationStrategy;

        // Interface for pagination strategy
        @FunctionalInterface
        private interface PaginationStrategy {
            String paginate(int limit, int offset);
        }

        Dialect(String jdbcPrefix, String driverClass, String dialectName,
                Function<String, String> identifierQuoter,
                PaginationStrategy paginationStrategy) {
            this.jdbcPrefix = jdbcPrefix;
            this.driverClass = driverClass;
            this.dialectName = dialectName;
            this.identifierQuoter = identifierQuoter;
            this.paginationStrategy = paginationStrategy;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public String getDialectName() {
            return dialectName;
        }

        public String quoteIdentifier(String identifier) {
            if (identifier == null || identifier.isEmpty()) {
                throw new IllegalArgumentException("Identifier cannot be null or empty");
            }
            return identifierQuoter.apply(identifier);
        }

        public String getPaginationClause(int limit, int offset) {
            if (limit < 0) {
                throw new IllegalArgumentException("Limit cannot be negative");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("Offset cannot be negative");
            }
            return paginationStrategy.paginate(limit, offset);
        }

        public boolean matchesUrl(String url) {
            return url != null && !url.isEmpty() && url.startsWith(jdbcPrefix);
        }
    }

    // Cache of dialects for faster lookup
    private static final Map<String, Dialect> PREFIX_TO_DIALECT_MAP = Stream.of(Dialect.values())
            .filter(d -> !d.equals(Dialect.UNKNOWN))
            .collect(Collectors.toMap(d -> d.jdbcPrefix, Function.identity()));

    /**
     * Determines the appropriate JDBC driver class based on the database URL.
     * 
     * @param url the JDBC connection URL
     * @return the driver class name or null if no matching driver is found
     * @throws IllegalArgumentException if the URL is null or empty
     */
    public static String getDriver(String url) {
        validateUrl(url);
        return findDialectByUrl(url).getDriverClass();
    }

    /**
     * Gets the dialect name based on the database URL.
     * 
     * @param url the JDBC connection URL
     * @return the dialect name or "unknown" if no matching dialect is found
     * @throws IllegalArgumentException if the URL is null or empty
     */
    public static String getDialectName(String url) {
        validateUrl(url);
        return findDialectByUrl(url).getDialectName();
    }

    /**
     * Quotes an identifier (table, column name) according to the dialect's rules.
     * 
     * @param identifier the identifier to quote
     * @param url        the JDBC connection URL
     * @return the properly quoted identifier
     * @throws IllegalArgumentException if the identifier or URL is null or empty
     */
    public static String quoteIdentifier(String identifier, String url) {
        validateUrl(url);
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        return findDialectByUrl(url).quoteIdentifier(identifier);
    }

    /**
     * Generates a pagination clause appropriate for the dialect.
     * 
     * @param url    the JDBC connection URL
     * @param limit  the maximum number of rows to return
     * @param offset the number of rows to skip
     * @return the SQL pagination clause
     * @throws IllegalArgumentException if the URL is null or empty, or if limit or
     *                                  offset is negative
     */
    public static String getPaginationClause(String url, int limit, int offset) {
        validateUrl(url);
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        return findDialectByUrl(url).getPaginationClause(limit, offset);
    }

    /**
     * Finds the dialect that matches the given URL.
     * 
     * @param url the JDBC connection URL
     * @return the matching dialect or UNKNOWN if no match is found
     */
    private static Dialect findDialectByUrl(String url) {
        return PREFIX_TO_DIALECT_MAP.entrySet().stream()
                .filter(entry -> url.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(Dialect.UNKNOWN);
    }

    /**
     * Validates that the URL is not null or empty.
     * 
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL is null or empty
     */
    private static void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("JDBC URL cannot be null or empty");
        }
    }
}