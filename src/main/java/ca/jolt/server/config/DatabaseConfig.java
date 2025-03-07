package ca.jolt.server.config;

import java.util.Properties;

/**
 * Represents the configuration settings for a database connection.
 * <p>
 * This class encapsulates database connection details such as URL, username,
 * password, and driver. It provides a factory method to create instances from
 * a {@link Properties} object, using default values if specific properties are
 * not provided.
 * </p>
 */
public class DatabaseConfig {

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
     * Default database driver used when not specified in properties.
     */
    private static final String DEFAULT_DRIVER = "Na/Default";

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
     * The JDBC driver class name for the database.
     */
    private final String driver;

    /**
     * Constructs a new DatabaseConfig with the specified settings.
     *
     * @param url      The database connection URL.
     * @param username The username for database authentication.
     * @param password The password for database authentication.
     * @param driver   The JDBC driver class name for the database.
     */
    private DatabaseConfig(String url, String username, String password, String driver) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
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
    public static DatabaseConfig fromProperties(Properties props) {
        String url = props.getProperty("db.url", DEFAULT_URL);
        String username = props.getProperty("db.username", DEFAULT_USERNAME);
        String password = props.getProperty("db.password", DEFAULT_PASSWORD);
        String driver = props.getProperty("db.driver", DEFAULT_DRIVER);
        return new DatabaseConfig(url, username, password, driver);
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

    /**
     * Returns the JDBC driver class name for the database.
     *
     * @return The driver class name used to establish the database connection.
     */
    public String getDriver() {
        return driver;
    }
}