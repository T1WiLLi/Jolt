package ca.jolt.database.schema;

import ca.jolt.database.models.TableMetadata;
import ca.jolt.database.models.TableMetadata.RelationMetadata;
import ca.jolt.database.models.TableMetadata.RelationType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Responsible for creating relationships such as join tables and foreign keys.
 */
public final class SchemaRelationsManager {
    private static final Logger logger = Logger.getLogger(SchemaRelationsManager.class.getName());

    private SchemaRelationsManager() {
        // Private constructor
    }

    /**
     * Creates join tables (for ManyToMany) and foreign keys (for ManyToOne).
     */
    public static <T> void createRelationships(Connection conn, TableMetadata<T> metadata) throws SQLException {
        for (String joinTableName : metadata.getJoinTables()) {
            if (!SchemaUtils.tableExists(conn, joinTableName)) {
                createJoinTable(conn, metadata, joinTableName);
            }
        }

        for (var entry : metadata.getRelationships().entrySet()) {
            RelationMetadata rel = entry.getValue();
            if (rel.getMappedBy().isEmpty() && rel.getType() == RelationType.MANY_TO_ONE) {
                SchemaUtils.addForeignKeyConstraint(
                        conn,
                        metadata.getTableName(),
                        rel.getJoinColumnName(),
                        SchemaUtils.getTargetTableName(rel.getTargetEntity()),
                        SchemaUtils.getForeignKeyIdColumn(rel.getTargetEntity()));
            }
        }
    }

    /**
     * Creates the join table for a ManyToMany relationship.
     */
    private static <T> void createJoinTable(Connection conn, TableMetadata<T> metadata, String joinTableName)
            throws SQLException {
        RelationMetadata rel = findManyToManyRelationship(metadata, joinTableName);
        if (rel == null) {
            logger.warning("Could not find ManyToMany metadata for join table: " + joinTableName);
            return;
        }

        String joinColumn = rel.getJoinColumn();
        String inverseJoinColumn = rel.getInverseJoinColumn();
        String targetTableName = SchemaUtils.toSnakeCase(rel.getTargetEntity().getSimpleName());
        String targetIdColumn = SchemaUtils.getForeignKeyIdColumn(rel.getTargetEntity());

        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(joinTableName).append(" (")
                .append(joinColumn).append(" INTEGER NOT NULL, ")
                .append(inverseJoinColumn).append(" INTEGER NOT NULL, ")
                .append("PRIMARY KEY (").append(joinColumn).append(", ").append(inverseJoinColumn).append("), ");

        createSql.append("CONSTRAINT fk_").append(joinTableName).append("_").append(joinColumn);
        createSql.append(" FOREIGN KEY (").append(joinColumn).append(") REFERENCES ");
        createSql.append(metadata.getTableName()).append("(").append(metadata.getIdColumn())
                .append(") ON DELETE CASCADE, ");

        createSql.append("CONSTRAINT fk_").append(joinTableName).append("_").append(inverseJoinColumn);
        createSql.append(" FOREIGN KEY (").append(inverseJoinColumn).append(") REFERENCES ");
        createSql.append(targetTableName).append("(").append(targetIdColumn).append(") ON DELETE CASCADE");

        createSql.append(")");

        SchemaUtils.executeStatement(conn, createSql.toString(),
                "Created join table: " + joinTableName,
                joinTableName);
    }

    /**
     * Finds a ManyToMany relationship that owns a given join table name.
     */
    private static <T> RelationMetadata findManyToManyRelationship(TableMetadata<T> metadata, String joinTableName) {
        for (RelationMetadata r : metadata.getRelationships().values()) {
            if (r.getType() == RelationType.MANY_TO_MANY &&
                    joinTableName.equals(r.getJoinTable()) &&
                    r.getMappedBy().isEmpty()) {
                return r;
            }
        }
        return null;
    }
}
