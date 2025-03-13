package ca.jolt.database;

import java.util.Properties;

/**
 * Represents the configuration settings for a database connection.
 * <p>
 * This class encapsulates database connection details such as URL, username,
 * password, and driver. It provides a factory method to create instances from
 * a {@link Properties} object, using default values if specific properties are
 * not provided.
 */
public final class DatabaseConfiguration {
    /**
     * Default database URL used when not specified in properties.
     */
    private static final String DEFAULT_URL = "Na/Default";

    /**
     * Default database username used when not specified in properties.
     */
    private static final String DEFAULT_USERNAME = "Na/Default";

    /**
     * Default database password used when not specified in properties.
     */
    private static final String DEFAULT_PASSWORD = "Na/Default";

    /**
     * Default database max connections used when not specified in properties.
     */
    private static final int DEFAULT_MAX_CONNECTIONS = 10;

    /**
     * The database connection URL.
     */
    private final String url;

    /**
     * The username for database authentication.
     */
    private final String username;

    /**
     * The password for database authentication.
     */
    private final String password;

    /**
     * The maximum number of connections to the database.
     */
    private final int maxConnections;

    /**
     * Constructs a new DatabaseConfig with the specified settings.
     *
     * @param url      The database connection URL.
     * @param username The username for database authentication.
     * @param password The password for database authentication.
     * @param driver   The JDBC driver class name for the database.
     */
    private DatabaseConfiguration(String url, String username, String password, int maxConnections) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
    }

    /**
     * Creates a DatabaseConfig instance from a properties object.
     * <p>
     * Retrieves database settings from the provided {@link Properties} object using
     * keys {@code db.url}, {@code db.username}, {@code db.password}, and
     * {@code db.driver}.
     * If a property is not found, the corresponding default value is used:
     * {@value #DEFAULT_URL}, {@value #DEFAULT_USERNAME},
     * {@value #DEFAULT_PASSWORD},
     * or {@value #DEFAULT_DRIVER}.
     * </p>
     *
     * @param props The {@link Properties} object containing database configuration
     *              settings.
     * @return A new {@code DatabaseConfig} instance with the loaded settings.
     */
    public static DatabaseConfiguration fromProperties(Properties props) {
        String url = props.getProperty("db.url", DEFAULT_URL);
        String username = props.getProperty("db.username", DEFAULT_USERNAME);
        String password = props.getProperty("db.password", DEFAULT_PASSWORD);
        int maxConnections = Integer
                .parseInt(props.getProperty("db.maxConnections", String.valueOf(DEFAULT_MAX_CONNECTIONS)));
        return new DatabaseConfiguration(url, username, password, maxConnections);
    }

    /**
     * Returns the database connection URL.
     *
     * @return The URL used to connect to the database.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the username for database authentication.
     *
     * @return The username used to authenticate with the database.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password for database authentication.
     *
     * @return The password used to authenticate with the database.
     */
    public String getPassword() {
        return password;
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}
