package ca.jolt.database;

import ca.jolt.database.exception.DatabaseException;
import ca.jolt.database.exception.DatabaseExceptionMapper;
import ca.jolt.database.models.TableMetadata;
import ca.jolt.database.schema.SchemaConstraintManager;
import ca.jolt.database.schema.SchemaCreator;
import ca.jolt.database.schema.SchemaIndexManager;
import ca.jolt.database.schema.SchemaRelationsManager;
import ca.jolt.database.schema.SchemaUpdater;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Facade class that orchestrates schema creation, updating, constraints,
 * indexes, and relationships.
 */
public final class SchemaManager {
    private static final Logger logger = Logger.getLogger(SchemaManager.class.getName());
    private static final Set<String> validatedTables = new HashSet<>();

    private SchemaManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validates (creates or updates) the table schema for the given entity, then
     * manages indexes, relationships, and constraints.
     */
    public static <T> void validateTable(Connection conn, TableMetadata<T> metadata) {
        String tableName = metadata.getTableName().toLowerCase();
        // if (!validatedTables.contains(tableName)) {
        // return;
        // }

        try {
            if (!tableExists(conn, metadata.getTableName())) {
                SchemaCreator.createTable(conn, metadata);
            } else {
                SchemaUpdater.updateTableSchema(conn, metadata);
            }

            SchemaIndexManager.manageIndexes(conn, metadata);
            SchemaRelationsManager.createRelationships(conn, metadata);
            SchemaConstraintManager.loadAllConstraints(conn, metadata.getTableName());

            validatedTables.add(tableName);
        } catch (SQLException e) {
            throw createDatabaseException(e,
                    "Schema validation for table " + metadata.getTableName(),
                    metadata.getTableName());
        }
    }

    /**
     * Checks if a table (case-insensitive) already exists in the database.
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null,
                tableName.toLowerCase(), new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    /**
     * Wraps a SQLException in a DatabaseException using the mapper.
     */
    private static DatabaseException createDatabaseException(SQLException e,
            String operation,
            String tableName) {
        DatabaseException dbException = DatabaseExceptionMapper.map(e, operation, tableName);
        logger.severe(() -> dbException.getTechnicalDetails());
        return dbException;
    }
}
