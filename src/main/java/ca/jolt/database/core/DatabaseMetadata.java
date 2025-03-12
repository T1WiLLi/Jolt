package ca.jolt.database.core;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.jolt.exceptions.JoltException;

/**
 * A utility class to expose metadata about the database.
 * It manages a reserved connection that is never given out for normal
 * operations.
 */
public class DatabaseMetadata {

    private static Connection reservedConnection;

    /**
     * Initializes the reserved connection.
     * This method should be called once after the Database bean has been created.
     *
     * @param config The DatabaseConfiguration from which to create the reserved
     *               connection.
     * @throws SQLException if the connection cannot be established.
     */
    public static synchronized void initialize(DatabaseConfiguration config) throws SQLException {
        if (reservedConnection == null || reservedConnection.isClosed()) {
            try {
                Class.forName(config.getDriver());
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver class not found: " + config.getDriver(), e);
            }
            reservedConnection = DriverManager.getConnection(
                    config.getUrl(),
                    config.getUsername(),
                    config.getPassword());
        }
    }

    /**
     * Returns a list of table names present in the current database.
     *
     * @return a List of table names.
     * @throws SQLException if there is an error querying the metadata.
     */
    public static List<String> getTables() throws SQLException {
        ensureReservedConnection();
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = reservedConnection.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    /**
     * Returns metadata about the columns of a specified table.
     *
     * @param tableName The name of the table.
     * @return a List of ColumnMetadata, one for each column.
     * @throws SQLException if there is an error querying the metadata.
     */
    public static List<ColumnMetadata> getColumns(String tableName) throws SQLException {
        ensureReservedConnection();
        List<ColumnMetadata> columns = new ArrayList<>();
        DatabaseMetaData metaData = reservedConnection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                columns.add(new ColumnMetadata(name, type, size, nullable));
            }
        }
        return columns;
    }

    /**
     * Closes the reserved connection.
     */
    public static synchronized void close() {
        if (reservedConnection != null) {
            try {
                reservedConnection.close();
            } catch (SQLException e) {
                throw new JoltException("Error closing reserved connection " + e.getMessage(), e);
            }
            reservedConnection = null;
        }
    }

    private static void ensureReservedConnection() throws SQLException {
        if (reservedConnection == null || reservedConnection.isClosed()) {
            throw new SQLException("Reserved connection is not available. Call DatabaseMetadata.initialize() first.");
        }
    }

    /**
     * A simple inner class representing metadata for a single column.
     */
    public static class ColumnMetadata {
        private final String name;
        private final String type;
        private final int size;
        private final boolean nullable;

        public ColumnMetadata(String name, String type, int size, boolean nullable) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.nullable = nullable;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public int getSize() {
            return size;
        }

        public boolean isNullable() {
            return nullable;
        }

        @Override
        public String toString() {
            return "ColumnMetadata{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", size=" + size +
                    ", nullable=" + nullable +
                    '}';
        }
    }
}