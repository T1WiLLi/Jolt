package ca.jolt.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ca.jolt.server.config.ConfigurationManager;

public class Database {
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
}
