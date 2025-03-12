package ca.jolt.database.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class ConnectionPool {

    private static final Logger logger = Logger.getLogger(ConnectionPool.class.getName());

    private final DatabaseConfiguration databaseConfiguration;
    private final BlockingQueue<Connection> pool;
    private final int poolSize;

    public ConnectionPool(DatabaseConfiguration config, int poolSize) {
        this.databaseConfiguration = config;
        this.poolSize = poolSize;
        this.pool = new LinkedBlockingQueue<Connection>(poolSize);
        initPool();
    }

    public ConnectionPool(DatabaseConfiguration config) {
        this(config, 5);
    }

    private void initPool() {
        try {
            Class.forName(databaseConfiguration.getDriver());

            for (int i = 0; i < poolSize; i++) {
                pool.offer(createNewConnection());
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to initialize the connection pool", e);
        }
    }

    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(
                databaseConfiguration.getUrl(),
                databaseConfiguration.getUsername(),
                databaseConfiguration.getPassword());
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = pool.take();
            if (!conn.isValid(2) || conn.isClosed()) {
                conn = createNewConnection();
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection.", e);
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (connection.isClosed() || !connection.isValid(2)) {
                connection = createNewConnection();
            }
            if (!pool.offer(connection, 3, TimeUnit.SECONDS)) {
                connection.close();
                logger.warning(() -> "Could not return connection to pool");
            }
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException ex) {
                logger.severe(() -> "Error closing connection: " + ex.getMessage());
            }
            logger.warning(() -> "Error releasing connection: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                connection.close();
            } catch (SQLException ex) {
                logger.severe(() -> "Interrupted while closing a connection: " + ex.getMessage());
            }
            logger.warning(() -> "Interrupted while waiting to offer a connection to the pool.");
        }
    }

    public void closeAll() {
        for (Connection conn : pool) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.severe(() -> "Failed to close all connection : " + e.getMessage());
            }
        }
        pool.clear();
    }
}