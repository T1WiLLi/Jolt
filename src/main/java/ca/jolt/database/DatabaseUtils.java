package ca.jolt.database;

import java.util.List;

public final class DatabaseUtils {

    public static final List<String> RESERVED_KEYWORDS = List.of(
            "ABSOLUTE", "ACTION", "ADD", "ADMIN", "AFTER", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY",
            "AS",
            "ASC", "ASSERTION", "ASYMMETRIC", "AT", "ATOMIC", "AUTHORIZATION", "BEFORE", "BEGIN", "BETWEEN", "BINARY",
            "BIT", "BLOB", "BOOLEAN", "BOTH", "BREADTH", "BY", "CALL", "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG",
            "CHAR", "CHARACTER", "CHECK", "CLOB", "CLOSE", "COLLATE", "COLLATION", "COLUMN", "COMMIT", "COMPLETION",
            "CONNECT", "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONSTRUCTOR", "CONTINUE", "CORRESPONDING", "CREATE",
            "CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP", "CURRENT_PATH",
            "CURRENT_ROLE",
            "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_TRANSFORM_GROUP_FOR_TYPE", "CURRENT_USER", "CURSOR", "CYCLE",
            "DATA", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED",
            "DELETE",
            "DEPTH", "DEREF", "DESC", "DESCRIBE", "DESCRIPTOR", "DETERMINISTIC", "DIAGNOSTICS", "DISCONNECT",
            "DISTINCT",
            "DO", "DOMAIN", "DOUBLE", "DROP", "DYNAMIC", "EACH", "ELSE", "ELSEIF", "END", "END-EXEC", "EQUALS",
            "ESCAPE",
            "EXCEPT", "EXCEPTION", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FALSE", "FETCH", "FILTER", "FIRST",
            "FLOAT", "FOR", "FOREIGN", "FOUND", "FREE", "FROM", "FULL", "FUNCTION", "GENERAL", "GET", "GLOBAL", "GO",
            "GOTO", "GRANT", "GROUP", "GROUPING", "HANDLER", "HAVING", "HOLD", "HOUR", "IDENTITY", "IF", "IMMEDIATE",
            "IN", "INDICATOR", "INITIALLY", "INNER", "INOUT", "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER",
            "INTERSECT", "INTERVAL", "INTO", "IS", "ISOLATION", "ITERATE", "JOIN", "KEY", "LANGUAGE", "LARGE", "LAST",
            "LATERAL", "LEADING", "LEAVE", "LEFT", "LEVEL", "LIKE", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOCATOR",
            "LOOP", "MAP", "MATCH", "MEMBER", "METHOD", "MINUTE", "MODIFIES", "MODULE", "MONTH", "NAMES", "NATIONAL",
            "NATURAL", "NCHAR", "NCLOB", "NEW", "NEXT", "NO", "NONE", "NOT", "NULL", "NUMERIC", "OBJECT",
            "OCTET_LENGTH",
            "OF", "OLD", "ON", "ONLY", "OPEN", "OPERATION", "OPTION", "OR", "ORDER", "ORDINALITY", "OUT", "OUTER",
            "OUTPUT", "OVER", "OVERLAPS", "PAD", "PARAMETER", "PARTIAL", "PARTITION", "PATH", "POSTFIX", "PRECISION",
            "PREFIX", "PREORDER", "PREPARE", "PRESERVE", "PRIMARY", "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC",
            "RANGE",
            "READ", "READS", "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "RELATIVE", "RELEASE", "REPEAT",
            "RESIGNAL", "RESTRICT", "RESULT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLE", "ROLLBACK", "ROLLUP",
            "ROUTINE", "ROW", "ROWS", "SAVEPOINT", "SCHEMA", "SCROLL", "SEARCH", "SECOND", "SECTION", "SELECT",
            "SENSITIVE", "SESSION", "SESSION_USER", "SET", "SETS", "SIGNAL", "SIMILAR", "SIZE", "SMALLINT", "SOME",
            "SPACE", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "START", "STATE",
            "STATIC", "STRUCTURE", "SYSTEM_USER", "TABLE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR",
            "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION", "TRANSLATION", "TREAT", "TRIGGER", "TRUE", "UNDER",
            "UNDO", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UNTIL", "UPDATE", "USAGE", "USER", "USING", "VALUE",
            "VALUES", "VARCHAR", "VARIABLE", "VARYING", "VIEW", "WHEN", "WHENEVER", "WHERE", "WHILE", "WINDOW", "WITH",
            "WITHOUT", "WORK", "WRITE", "YEAR", "ZONE",

            // PostgreSQL-specific
            "ANALYZE", "ANALYSE", "BETWEEN", "BINARY", "CROSS", "CURRENT_CATALOG", "CURRENT_SCHEMA", "DO", "FREEZE",
            "FULL", "ILIKE", "INNER", "ISNULL", "JOIN", "LEFT", "LIKE", "LIMIT", "NATURAL", "NOTNULL", "NULLS", "OUTER",
            "OVERLAPS", "RIGHT", "SIMILAR", "TABLESAMPLE", "VERBOSE", "WINDOW",

            // MySQL-specific
            "AUTO_INCREMENT", "BINARY", "BLOB", "CHANGE", "DATABASES", "DELAYED", "DISTINCTROW", "DIV", "ENCLOSED",
            "EXPLAIN", "FLOAT4", "FLOAT8", "FORCE", "HIGH_PRIORITY", "IGNORE", "INDEX", "INFILE", "INT1", "INT2",
            "INT3",
            "INT4", "INT8", "KEY", "KEYS", "KILL", "LOW_PRIORITY", "MEDIUMINT", "MIDDLEINT", "MOD",
            "NO_WRITE_TO_BINLOG",
            "OPTIMIZE", "OPTIONALLY", "OUTER", "PURGE", "REGEXP", "RENAME", "REPLACE", "REQUIRE", "ROWS", "SCHEMAS",
            "SEPARATOR", "SHOW", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STRAIGHT_JOIN",
            "TERMINATED", "TINYBLOB", "TINYTEXT", "UNDO", "UNLOCK", "UNSIGNED", "USAGE", "USE", "USING", "UTC_DATE",
            "UTC_TIME", "UTC_TIMESTAMP", "VARBINARY", "XOR", "ZEROFILL",

            // SQL Server-specific
            "BACKUP", "BREAK", "BROWSE", "BULK", "CHECKPOINT", "CLUSTERED", "COMMITTED", "CONTAINS", "CONTAINSTABLE",
            "CONTINUE", "DATA_COMPRESSION", "DBCC", "DISK", "DISTRIBUTED", "DUMP", "ERRLVL", "ERROR", "EXIT",
            "FASTFIRSTROW", "FILLFACTOR", "FLOPPY", "FORMSOF", "FREETEXT", "FREETEXTTABLE", "HOLDLOCK",
            "IDENTITY_INSERT", "INDEX", "ISOLATION", "KILL", "LINENO", "LOAD", "MAXDOP", "MINUTE", "MIRROREXIT",
            "NONCLUSTERED", "NOTHING", "NOCOUNT", "NOFORMAT", "NOINIT", "NOWAIT", "OFFSETS", "OPENQUERY", "OPENROWSET",
            "OPENDATASOURCE", "OVER", "PERCENT", "PLAN", "PRECISION", "PROC", "RAISERROR", "READTEXT", "RECONFIGURE",
            "REPEATABLE", "REPLICATION", "RESTORE", "RESTRICT", "RETURN", "ROWCOUNT", "ROWGUIDCOL",
            "SEMANTICKEYPHRASETABLE", "SEMANTICSIMILARITYDETAILSTABLE", "SEMANTICSIMILARITYTABLE", "SERIALIZABLE",
            "SHUTDOWN", "STATISTICS", "TABLESAMPLE", "TAPE", "TEXTSIZE", "TOP", "TRAN", "TRUNCATE", "TSEQUAL",
            "UNCOMMITTED", "UPDATETEXT", "USE", "WAITFOR", "WITHIN GROUP", "WORK", "WRITETEXT");

    public static boolean isReservedKeyword(String identifier) {
        return RESERVED_KEYWORDS.contains(identifier.toUpperCase());
    }
}
