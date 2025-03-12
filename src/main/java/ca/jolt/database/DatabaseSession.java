package ca.jolt.database;

import java.sql.Connection;
import java.sql.SQLException;

import ca.jolt.database.core.Database;
import ca.jolt.database.core.DatabaseTransaction;
import lombok.Getter;

public final class DatabaseSession implements AutoCloseable {

    private final Database database;
    @Getter
    private final Connection connection;
    private boolean closed = false;

    public DatabaseSession(Database database, Connection connection) {
        this.database = database;
        this.connection = connection;
    }

    public DatabaseTransaction beginTransaction() throws SQLException {
        DatabaseTransaction transaction = new DatabaseTransaction(this);
        transaction.begin();
        return transaction;
    }

    @Override
    public void close() throws SQLException {
        if (!closed) {
            database.releaseConnection(connection);
            closed = true;
        }
    }
}
