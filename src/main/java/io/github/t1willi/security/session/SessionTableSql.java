package io.github.t1willi.security.session;

final class SessionTableSql {
        public static final String SESSION_TABLE = "session";
        public static final String ATTRS_TABLE = "session_attributes";

        public static final String CREATE_SESSION_TABLE = "CREATE TABLE IF NOT EXISTS " + SESSION_TABLE + " (" +
                        "  id VARCHAR(255) PRIMARY KEY," +
                        "  last_access BIGINT NOT NULL," +
                        "  max_inactive INT NOT NULL," +
                        "  expiry_time BIGINT," +
                        "  ip_address VARCHAR(45)," +
                        "  user_agent VARCHAR(512)," +
                        "  created_time BIGINT NOT NULL" +
                        ");";

        public static final String CREATE_ATTRS_TABLE = "CREATE TABLE IF NOT EXISTS " + ATTRS_TABLE + " (" +
                        "  session_id VARCHAR(255) NOT NULL," +
                        "  name VARCHAR(255) NOT NULL," +
                        "  value TEXT," +
                        "  PRIMARY KEY (session_id, name)," +
                        "  FOREIGN KEY (session_id) REFERENCES " + SESSION_TABLE + "(id) ON DELETE CASCADE" +
                        ");";

        public static final String CREATE_INDEX_EXPIRY = "CREATE INDEX IF NOT EXISTS idx_" + SESSION_TABLE + "_expiry "
                        + "ON " + SESSION_TABLE + "(expiry_time);";

        public static final String CREATE_INDEX_LAST_ACCESS = "CREATE INDEX IF NOT EXISTS idx_" + SESSION_TABLE
                        + "_last_access "
                        + "ON " + SESSION_TABLE + "(last_access);";

        public static final String CREATE_INDEX_CREATED_TIME = "CREATE INDEX IF NOT EXISTS idx_" + SESSION_TABLE
                        + "_created_time "
                        + "ON " + SESSION_TABLE + "(created_time);";

        public static final String CREATE_INDEX_ATTRS_SESSION_ID = "CREATE INDEX IF NOT EXISTS idx_" + ATTRS_TABLE
                        + "_session_id "
                        + "ON " + ATTRS_TABLE + "(session_id);";

        public static final String CREATE_INDEX_ATTRS_NAME = "CREATE INDEX IF NOT EXISTS idx_" + ATTRS_TABLE + "_name "
                        + "ON " + ATTRS_TABLE + "(name);";

        public static final String UPSERT_SESSION = "INSERT INTO " + SESSION_TABLE +
                        " (id, last_access, max_inactive, expiry_time, ip_address, user_agent, created_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET " +
                        "  last_access  = EXCLUDED.last_access, " +
                        "  max_inactive = EXCLUDED.max_inactive, " +
                        "  expiry_time  = EXCLUDED.expiry_time, " +
                        "  ip_address   = EXCLUDED.ip_address, " +
                        "  user_agent   = EXCLUDED.user_agent";

        public static final String DELETE_SESSION = "DELETE FROM " + SESSION_TABLE + " WHERE id = ?";

        public static final String SELECT_SESSION = "SELECT last_access, max_inactive, expiry_time, created_time, ip_address, user_agent "
                        +
                        "FROM " + SESSION_TABLE + " WHERE id = ?";

        public static final String INSERT_ATTR = "INSERT INTO " + ATTRS_TABLE +
                        " (session_id, name, value) VALUES (?, ?, ?)";

        public static final String UPDATE_ATTR = "UPDATE " + ATTRS_TABLE +
                        " SET value = ? WHERE session_id = ? AND name = ?";

        public static final String DELETE_ATTR = "DELETE FROM " + ATTRS_TABLE +
                        " WHERE session_id = ? AND name = ?";

        public static final String SELECT_ATTRS = "SELECT name, value FROM " + ATTRS_TABLE + " WHERE session_id = ?";
}
