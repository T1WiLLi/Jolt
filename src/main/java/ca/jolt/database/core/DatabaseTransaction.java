package ca.jolt.database.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Stack;

import ca.jolt.database.DatabaseSession;
import lombok.Getter;

public final class DatabaseTransaction implements AutoCloseable {
    private final DatabaseSession session;
    private final Connection connection;
    private final Stack<Savepoint> savepoints = new Stack<Savepoint>();
    @Getter
    private int transactionLevel = 0;
    private boolean completed = false;

    public DatabaseTransaction(DatabaseSession session) {
        this.session = session;
        this.connection = session.getConnection();
    }

    public void begin() throws SQLException {
        if (transactionLevel == 0) {
            connection.setAutoCommit(false);
        } else {
            Savepoint sp = connection.setSavepoint("SP_" + transactionLevel);
            savepoints.push(sp);
        }
        transactionLevel++;
    }

    public void commit() throws SQLException {
        if (transactionLevel == 0) {
            throw new SQLException("Cannot commit a transaction that has not been started");
        }

        transactionLevel--;
        if (transactionLevel == 0) {
            connection.commit();
            connection.setAutoCommit(true);
            completed = true;
        } else {
            connection.releaseSavepoint(savepoints.pop());
        }
    }

    public void rollback() throws SQLException {
        if (transactionLevel == 0) {
            throw new SQLException("Cannot rollback a transaction that has not been started");
        }

        if (transactionLevel == 1) {
            connection.rollback();
            connection.setAutoCommit(true);
            transactionLevel = 0;
            savepoints.clear();
            completed = true;
        } else {
            connection.rollback(savepoints.pop());
            transactionLevel--;
        }
    }

    @Override
    public void close() throws SQLException {
        if (!completed) {
            rollback();
        }
        session.close();
    }
}