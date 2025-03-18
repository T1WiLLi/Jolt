package ca.jolt.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ca.jolt.server.config.ConfigurationManager;

public final class Database {
    private static Database instance;
    private HikariDataSource dataSource;
    private static final Logger logger = Logger.getLogger(Database.class.getName());
    private boolean initialized = false;

    private Database() {
        try {
            DatabaseConfiguration config = ConfigurationManager.getInstance().getDatabaseConfig();

            if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
                logger.warning("Database URL not configured. Database functionality will be disabled.");
                return;
            }

            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                logger.warning("PostgreSQL driver not found in classpath. Database functionality will be disabled.");
                return;
            }

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(config.getMaxConnections());

            logger.info("Initializing database connection pool");
            this.dataSource = new HikariDataSource(hikariConfig);
            this.initialized = true;
            logger.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            logger.warning(() -> "Failed to initialize database: " + e.getMessage() +
                    ". Database functionality will be disabled.");
        }
    }

    public static synchronized void init() {
        if (instance == null) {
            instance = new Database();
        }
    }

    public static Database getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Database instance has not been initialized");
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null) {
            throw new SQLException("Database is not initialized or disabled");
        }
        return dataSource.getConnection();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning(() -> "Error closing database connection: " + e.getMessage());
        }
    }

    public void close() {
        if (initialized && dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Injects a variable into the database configuration.
     * 
     * @param key   The variable name
     * @param value The variable value
     * @return true if successful
     */
    public static boolean injectVariable(String key, String value) {
        return DatabaseMetadata.injectVariable(key, value);
    }

    /**
     * Gets a variable value from the database configuration.
     * 
     * @param key The variable name
     * @return The variable value or null if not found
     */
    public static String getVariable(String key) {
        return DatabaseMetadata.getVariable(key);
    }

    /**
     * Gets all application variables from the database.
     * 
     * @return Map of variable names to values
     */
    public static Map<String, String> getAllVariables() {
        return DatabaseMetadata.getAllVariables();
    }

    /**
     * Creates or replaces a database trigger function.
     * 
     * @param name         Function name
     * @param functionBody PL/pgSQL function body
     * @return true if successful
     */
    public static boolean createTriggerFunction(String name, String functionBody) {
        return DatabaseMetadata.createTriggerFunction(name, functionBody, null, "trigger");
    }

    /**
     * Creates or replaces a database view.
     * 
     * @param name  View name
     * @param query SQL query that defines the view
     * @return true if successful
     */
    public static boolean createView(String name, String query) {
        return DatabaseMetadata.createView(name, query);
    }

    /**
     * Creates or replaces a database function.
     * 
     * @param name         Function name
     * @param functionBody Function body
     * @param returnType   Return type
     * @return true if successful
     */
    public static boolean createFunction(String name, String functionBody, String returnType) {
        return DatabaseMetadata.createFunction(name, functionBody, null, returnType, "plpgsql", false, "VOLATILE");
    }

}
