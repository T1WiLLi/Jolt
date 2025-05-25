package io.github.t1willi.security.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;

/**
 * Enhanced JDBC-backed Store for Tomcat sessions with transactional operations,
 * batch cleanup, optimized connection usage, and comprehensive logging.
 */
public class JoltJDBCStore extends StoreBase {
    private static final Log log = LogFactory.getLog(JoltJDBCStore.class);

    private static final String DEFAULT_TABLE = Constant.Security.DEFAULT_SESSION_TABLE_NAME;
    private static final int DEFAULT_BATCH_SIZE = 25; // default batch size for cleanup
    private static final int DEFAULT_CLEANUP_INTERVAL = 300; // 5 minutes

    private String sessionTable;
    private Database database;
    private int batchSize;
    private int cleanupInterval;
    private ScheduledExecutorService cleanupExecutor;

    @Override
    public void setManager(Manager manager) {
        super.setManager(manager);

        try {
            this.database = Database.getInstance();
            if (!database.isInitialized()) {
                throw new IllegalStateException("Database not initialized");
            }

            ConfigurationManager config = ConfigurationManager.getInstance();
            this.sessionTable = config.getProperty("session.table", DEFAULT_TABLE);
            this.batchSize = Integer
                    .parseInt(config.getProperty("session.batch.size", String.valueOf(DEFAULT_BATCH_SIZE)));
            this.cleanupInterval = Integer
                    .parseInt(config.getProperty("session.cleanup.interval", String.valueOf(DEFAULT_CLEANUP_INTERVAL)));

            log.info("Initializing JoltJDBCStore with table: " + sessionTable +
                    ", batch size: " + batchSize + ", cleanup interval: " + cleanupInterval + "s");

            createTableIfNotExists();
            createIndexesIfNotExists();
            cleanupExpiredSessions();
            startPeriodicCleanup();

            log.info("JoltJDBCStore initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JoltJDBCStore", e);
            throw new RuntimeException("JoltJDBCStore initialization failed", e);
        }
    }

    private void createTableIfNotExists() {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s ("
                        + "id VARCHAR(255) PRIMARY KEY, "
                        + "data BYTEA NOT NULL, "
                        + "last_access BIGINT, "
                        + "max_inactive INT, "
                        + "expiry_time BIGINT, "
                        + "ip_address VARCHAR(255), "
                        + "user_agent TEXT, "
                        + "created_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000, "
                        + "updated_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000)",
                sessionTable);

        executeWithTransaction(sql, stmt -> {
            stmt.executeUpdate();
            log.debug("Session table created or verified: " + sessionTable);
        });
    }

    private void createIndexesIfNotExists() {
        String[] indexes = {
                String.format("CREATE INDEX IF NOT EXISTS idx_%1$s_expiry ON %1$s(expiry_time)", sessionTable),
                String.format("CREATE INDEX IF NOT EXISTS idx_%1$s_last_access ON %1$s(last_access)", sessionTable),
        };

        executeWithTransaction(conn -> {
            try (Statement stmt = conn.createStatement()) {
                for (String indexSql : indexes) {
                    stmt.executeUpdate(indexSql);
                }
                log.debug("Session table indexes created or verified");
            }
        });
    }

    private void startPeriodicCleanup() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }

        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionCleanup-" + sessionTable);
            t.setDaemon(true);
            return t;
        });

        cleanupExecutor.scheduleWithFixedDelay(
                this::batchCleanupExpiredSessions,
                cleanupInterval,
                cleanupInterval,
                TimeUnit.SECONDS);

        log.info("Started periodic session cleanup with interval: " + cleanupInterval + "s");
    }

    /**
     * Batch cleanup of expired sessions for better performance
     */
    private void batchCleanupExpiredSessions() {
        try {
            long startTime = System.currentTimeMillis();
            int deletedCount = 0;

            String selectSql = "SELECT id FROM " + sessionTable +
                    " WHERE expiry_time > 0 AND expiry_time < ? LIMIT ?";
            String deleteSql = "DELETE FROM " + sessionTable + " WHERE id = ANY(?)";

            executeWithTransaction(conn -> {
                List<String> expiredIds = new ArrayList<>();

                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setLong(1, System.currentTimeMillis());
                    selectStmt.setInt(2, batchSize * 10);

                    try (ResultSet rs = selectStmt.executeQuery()) {
                        while (rs.next()) {
                            expiredIds.add(rs.getString("id"));
                        }
                    }
                }

                if (!expiredIds.isEmpty()) {
                    for (int i = 0; i < expiredIds.size(); i += batchSize) {
                        int endIndex = Math.min(i + batchSize, expiredIds.size());
                        List<String> batch = expiredIds.subList(i, endIndex);

                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            String[] idsArray = batch.toArray(new String[0]);
                            deleteStmt.setArray(1, conn.createArrayOf("VARCHAR", idsArray));
                            deleteStmt.executeUpdate();
                        }
                    }
                }
            });

            long duration = System.currentTimeMillis() - startTime;
            if (deletedCount > 0) {
                log.info(
                        "Batch cleanup completed: " + deletedCount + " expired sessions deleted in " + duration + "ms");
            } else {
                log.debug("Batch cleanup completed: no expired sessions found");
            }

        } catch (Exception e) {
            log.error("Error during batch cleanup of expired sessions", e);
        }
    }

    /**
     * Initial cleanup of expired sessions during startup
     */
    private void cleanupExpiredSessions() {
        String sql = "DELETE FROM " + sessionTable + " WHERE expiry_time > 0 AND expiry_time < ?";

        try {
            executeWithTransaction(sql, stmt -> {
                stmt.setLong(1, System.currentTimeMillis());
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    log.info("Startup cleanup: removed " + deleted + " expired sessions");
                }
            });
        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions during startup", e);
        }
    }

    @Override
    public int getSize() throws IOException {
        String sql = "SELECT COUNT(*) FROM " + sessionTable + " WHERE (expiry_time <= 0 OR expiry_time > ?)";

        try {
            return executeWithResult(sql, stmt -> {
                stmt.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            });
        } catch (Exception e) {
            log.error("Failed to get session count", e);
            throw new IOException("Failed to get session count", e);
        }
    }

    @Override
    public String[] keys() throws IOException {
        String sql = "SELECT id FROM " + sessionTable + " WHERE (expiry_time <= 0 OR expiry_time > ?)";

        try {
            return executeWithResult(sql, stmt -> {
                stmt.setLong(1, System.currentTimeMillis());
                ArrayList<String> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(rs.getString("id"));
                    }
                }
                return list.toArray(new String[0]);
            });
        } catch (Exception e) {
            log.error("Failed to get session keys", e);
            throw new IOException("Failed to get session keys", e);
        }
    }

    @Override
    public Session load(String id) throws IOException {
        if (id == null || id.isEmpty()) {
            return null;
        }

        log.debug("Loading session: " + id);

        String sql = "SELECT data, max_inactive, expiry_time FROM " + sessionTable + " WHERE id = ?";

        try {
            return executeWithResult(sql, stmt -> {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        log.debug("Session not found: " + id);
                        return null;
                    }

                    long expiry = rs.getLong("expiry_time");
                    long currentTime = System.currentTimeMillis();

                    if (expiry > 0 && currentTime > expiry) {
                        log.debug("Session expired: " + id + " (expired at: " + expiry + ", current: " + currentTime
                                + ")");
                        scheduleExpiredSessionCleanup(id);
                        return null;
                    }

                    byte[] blob = rs.getBytes("data");
                    StandardSession session = (StandardSession) getManager().createEmptySession();
                    session.setId(id, false);
                    session.setMaxInactiveInterval(rs.getInt("max_inactive"));

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(blob)) {
                        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                            session.readObjectData(ois);
                        }
                    } catch (IOException | ClassNotFoundException ex) {
                        log.error("Exception while reading session object data for id: " + id, ex);
                        return null;
                    }

                    session.setManager(getManager());
                    session.setId(id, true);

                    log.debug("Session loaded successfully: " + id);
                    return session;
                }
            });
        } catch (Exception e) {
            log.error("Failed to load session: " + id, e);
            throw new IOException("Failed to load session", e);
        }
    }

    @Override
    public void save(Session sess) throws IOException {
        if (!(sess instanceof StandardSession)) {
            log.warn("Attempted to save non-StandardSession: " + sess.getClass().getName());
            return;
        }

        StandardSession s = (StandardSession) sess;
        String sessionId = s.getId();

        log.debug("Saving session: " + sessionId);

        if (!s.isValid()) {
            log.debug("Session invalid, deleting: " + sessionId);
            deleteById(sessionId);
            return;
        }

        try {
            byte[] blob;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                s.writeObjectData(oos);
                oos.flush();
                blob = baos.toByteArray();
            }

            String sql = String.format(
                    "INSERT INTO %s (id, data, last_access, max_inactive, expiry_time, ip_address, user_agent, updated_time) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT (id) DO UPDATE SET " +
                            "data = EXCLUDED.data, " +
                            "max_inactive = EXCLUDED.max_inactive, " +
                            "expiry_time = EXCLUDED.expiry_time, " +
                            "last_access = EXCLUDED.last_access, " +
                            "ip_address = EXCLUDED.ip_address, " +
                            "user_agent = EXCLUDED.user_agent, " +
                            "updated_time = EXCLUDED.updated_time",
                    sessionTable);

            executeWithTransaction(sql, stmt -> {
                stmt.setString(1, sessionId);
                stmt.setBytes(2, blob);
                stmt.setLong(3, s.getLastAccessedTime());
                stmt.setInt(4, s.getMaxInactiveInterval());

                long expiry = s.getMaxInactiveInterval() > 0
                        ? s.getLastAccessedTime() + (s.getMaxInactiveInterval() * 1000L)
                        : -1;
                stmt.setLong(5, expiry);

                stmt.setString(6, (String) s.getAttribute(io.github.t1willi.security.session.Session.KEY_IP_ADDRESS));
                stmt.setString(7, (String) s.getAttribute(io.github.t1willi.security.session.Session.KEY_USER_AGENT));
                stmt.setLong(8, System.currentTimeMillis());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    log.debug("Session saved successfully: " + sessionId);
                } else {
                    log.warn("No rows affected when saving session: " + sessionId);
                }
            });

        } catch (Exception e) {
            log.error("Failed to save session: " + sessionId, e);
            throw new IOException("Failed to save session", e);
        }
    }

    private void deleteById(String id) throws IOException {
        String sql = "DELETE FROM " + sessionTable + " WHERE id = ?";

        try {
            executeWithTransaction(sql, stmt -> {
                stmt.setString(1, id);
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    log.debug("Session deleted: " + id);
                }
            });
        } catch (Exception e) {
            log.error("Failed to delete session: " + id, e);
            throw new IOException("Failed to delete session", e);
        }
    }

    @Override
    public void remove(String id) throws IOException {
        if (id == null || id.isEmpty()) {
            return;
        }

        log.debug("Removing session: " + id);

        String checkSql = "SELECT expiry_time FROM " + sessionTable + " WHERE id = ?";

        try {
            Boolean shouldDelete = executeWithResult(checkSql, stmt -> {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    long expiry = rs.getLong("expiry_time");
                    return expiry > 0 && expiry < System.currentTimeMillis();
                }
            });

            if (shouldDelete) {
                deleteById(id);
            } else {
                log.debug("Session not expired or not found, skipping removal: " + id);
            }
        } catch (Exception e) {
            log.error("Failed to remove session: " + id, e);
            try {
                deleteById(id);
            } catch (IOException deleteException) {
                log.error("Failed to delete session after check failure: " + id, deleteException);
            }
        }
    }

    @Override
    public void clear() throws IOException {
        log.info("Clearing all sessions from table: " + sessionTable);

        String sql = "DELETE FROM " + sessionTable;

        try {
            executeWithTransaction(sql, stmt -> {
                int affected = stmt.executeUpdate();
                log.info("Cleared " + affected + " sessions from table: " + sessionTable);
            });
        } catch (Exception e) {
            log.error("Failed to clear sessions", e);
            throw new IOException("Failed to clear sessions", e);
        }
    }

    private void scheduleExpiredSessionCleanup(String sessionId) {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.submit(() -> {
                try {
                    deleteById(sessionId);
                    log.debug("Cleaned up expired session: " + sessionId);
                } catch (IOException e) {
                    log.warn("Failed to cleanup expired session: " + sessionId, e);
                }
            });
        }
    }

    @FunctionalInterface
    private interface TransactionalOperation {
        void execute(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    private interface PreparedStatementOperation {
        void execute(PreparedStatement stmt) throws SQLException;
    }

    @FunctionalInterface
    private interface PreparedStatementFunction<T> {
        T execute(PreparedStatement stmt) throws SQLException;
    }

    private void executeWithTransaction(TransactionalOperation operation) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                operation.execute(conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    private void executeWithTransaction(String sql, PreparedStatementOperation operation) {
        executeWithTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                operation.execute(stmt);
            }
        });
    }

    private <T> T executeWithResult(String sql, PreparedStatementFunction<T> function) throws SQLException {
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            return function.execute(stmt);
        }
    }

    public void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}