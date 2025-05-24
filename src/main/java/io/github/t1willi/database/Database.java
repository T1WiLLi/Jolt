package io.github.t1willi.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.t1willi.server.config.ConfigurationManager;

/**
 * The `Database` class is a singleton responsible for managing the database
 * connection pool
 * using HikariCP. It provides methods to initialize the connection pool,
 * retrieve connections,
 * and release resources. It also supports injecting and retrieving application
 * variables
 * from the database configuration.
 *
 * This class ensures thread-safe initialization and lazy loading of the
 * database connection pool.
 */
public final class Database {
    private static Database instance; // Singleton instance of the Database class
    private HikariDataSource dataSource; // HikariCP data source for connection pooling
    private static final Logger logger = Logger.getLogger(Database.class.getName()); // Logger for database operations
    private boolean initialized = false; // Flag to track if the database is initialized

    /**
     * Private constructor to enforce singleton pattern. Initializes the database
     * connection pool
     * using the configuration provided by `ConfigurationManager`.
     */
    private Database() {
        try {
            DatabaseConfiguration config = ConfigurationManager.getInstance().getDatabaseConfig();

            // Check if the database URL is configured
            if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
                logger.warning("Database URL not configured. Database functionality will be disabled.");
                return;
            }

            // Configure HikariCP connection pool
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(config.getMaxConnections());

            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);

            // Initialize the connection pool
            logger.info("Initializing database connection pool");
            this.dataSource = new HikariDataSource(hikariConfig);
            this.initialized = true;
            logger.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            logger.warning(() -> "Failed to initialize database: " + e.getMessage() +
                    ". Database functionality will be disabled.");
        }
    }

    /**
     * Initializes the singleton instance of the `Database` class. This method must
     * be called
     * before any other methods in this class are used.
     */
    public static synchronized void init() {
        if (instance == null) {
            instance = new Database();
        }
    }

    /**
     * Returns the singleton instance of the `Database` class.
     *
     * @return The singleton instance of the `Database` class.
     * @throws IllegalStateException If the database has not been initialized.
     */
    public static Database getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Database instance has not been initialized");
        }
        return instance;
    }

    /**
     * Retrieves a database connection from the connection pool.
     *
     * @return A database connection.
     * @throws SQLException If the database is not initialized or a connection
     *                      cannot be obtained.
     */
    public Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null) {
            throw new SQLException("Database is not initialized or disabled");
        }
        return dataSource.getConnection();
    }

    /**
     * Checks if the database connection pool has been successfully initialized.
     *
     * @return `true` if the database is initialized, `false` otherwise.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Releases a database connection back to the connection pool.
     *
     * @param connection The database connection to release.
     */
    public void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning(() -> "Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Retrieves the HikariCP data source.
     *
     * @return The HikariCP data source.
     * @throws IllegalStateException If the database has not been initialized.
     */
    public DataSource getDataSource() {
        if (!initialized)
            throw new IllegalStateException("DB not initialized");
        return dataSource;
    }

    /**
     * Closes the database connection pool and releases all resources.
     */
    public void close() {
        if (initialized && dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Injects a variable into the database configuration.
     *
     * @param key   The name of the variable to inject.
     * @param value The value of the variable to inject.
     * @return `true` if the variable was successfully injected, `false` otherwise.
     */
    public static boolean injectVariable(String key, String value) {
        return DatabaseMetadata.injectVariable(key, value);
    }

    /**
     * Retrieves the value of a variable from the database configuration.
     *
     * @param key The name of the variable to retrieve.
     * @return The value of the variable, or `null` if the variable does not exist.
     */
    public static String getVariable(String key) {
        return DatabaseMetadata.getVariable(key);
    }

    /**
     * Retrieves all application variables from the database configuration.
     *
     * @return A map of variable names to their values.
     */
    public static Map<String, String> getAllVariables() {
        return DatabaseMetadata.getAllVariables();
    }
}