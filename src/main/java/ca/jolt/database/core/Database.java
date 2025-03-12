package ca.jolt.database.core;

import java.sql.Connection;
import java.sql.SQLException;

import ca.jolt.database.DatabaseSession;
import ca.jolt.server.config.ConfigurationManager;
import lombok.Getter;

public final class Database {
    @Getter
    private final DatabaseConfiguration configuration;
    private final ConnectionPool connectionPool;

    public Database() {
        this.configuration = ConfigurationManager.getInstance().getDatabaseConfig();
        this.connectionPool = new ConnectionPool(configuration);
    }

    public DatabaseSession getSession() throws SQLException {
        Connection conn = connectionPool.getConnection();
        return new DatabaseSession(this, conn);
    }

    public void releaseConnection(Connection connection) {
        connectionPool.releaseConnection(connection);
    }

    public void close() {
        connectionPool.closeAll();
    }
}
