package ca.jolt.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ca.jolt.server.config.ConfigurationManager;

public class Database {
    private static Database instance;
    private final HikariDataSource dataSource;

    private Database() {
        DatabaseConfiguration config = ConfigurationManager.getInstance().getDatabaseConfig();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getMaxConnections());
        this.dataSource = new HikariDataSource(hikariConfig);
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
        return dataSource.getConnection();
    }

    public void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
