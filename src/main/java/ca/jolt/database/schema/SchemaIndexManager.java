package ca.jolt.database.schema;

import ca.jolt.database.models.TableMetadata;
import ca.jolt.database.models.TableMetadata.RelationMetadata;
import ca.jolt.database.models.TableMetadata.RelationType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Responsible for creating/managing indexes on tables.
 */
public final class SchemaIndexManager {
    private static final Logger logger = Logger.getLogger(SchemaIndexManager.class.getName());

    private SchemaIndexManager() {
        // Private constructor
    }

    /**
     * Creates indexes for columns specified in @Table(indexes={...}) plus
     * foreign key columns.
     */
    public static <T> void manageIndexes(Connection conn, TableMetadata<T> metadata) throws SQLException {
        for (String indexCol : metadata.getIndexes()) {
            if (metadata.getUnique().contains(indexCol) ||
                    indexCol.equals(metadata.getFieldToColumn().get(metadata.getId().getName()))) {
                continue;
            }
            createIndex(conn, metadata.getTableName(), indexCol);
        }

        for (RelationMetadata rel : metadata.getRelationships().values()) {
            if (rel.getType() == RelationType.MANY_TO_ONE && rel.getMappedBy().isEmpty()) {
                createIndex(conn, metadata.getTableName(), rel.getJoinColumnName());
            }
        }
    }

    /**
     * Creates an index on the specified column (if not already existing).
     */
    private static void createIndex(Connection conn, String tableName, String columnName) throws SQLException {
        String indexName = "idx_" + tableName + "_" + columnName;
        String createIdxSql = "CREATE INDEX IF NOT EXISTS " + indexName +
                " ON " + tableName + " (" + columnName + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createIdxSql);
            logger.fine("Created index " + indexName + " on table " + tableName);
        } catch (SQLException e) {
            logger.warning("Could not create index for column '" + columnName + "': " + e.getMessage());
        }
    }
}
