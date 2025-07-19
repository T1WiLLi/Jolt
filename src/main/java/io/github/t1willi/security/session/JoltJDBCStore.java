package io.github.t1willi.security.session;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;
import io.github.t1willi.utils.JacksonUtil;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class JoltJDBCStore extends StoreBase {
    private static final Log log = LogFactory.getLog(JoltJDBCStore.class);

    private final Database db = Database.getInstance();
    private ScheduledExecutorService cleanupExec;
    private int cleanupInterval;

    @Override
    public void setManager(Manager manager) {
        super.setManager(manager);
        initializeCleanupInterval();
        createDatabaseTables();
        startCleanupScheduler();
    }

    @Override
    public Session load(String id) throws IOException {
        if (id == null || id.isEmpty())
            return null;

        StandardSession session = createEmptySession(id);
        loadSessionMetadata(session, id);
        loadSessionAttributes(session, id);
        return session;
    }

    @Override
    public void save(Session session) throws IOException {
        if (!(session instanceof StandardSession))
            return;
        StandardSession ss = (StandardSession) session;
        if (!ss.isValid()) {
            deleteById(ss.getId());
            return;
        }
        saveSessionToDatabase(ss);
    }

    @Override
    public void processExpires() {
        cleanupExpired();
    }

    @Override
    public int getSize() throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = prepareSizeQuery(c);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("getSize() failed", e);
            throw new IOException(e);
        }
    }

    @Override
    public String[] keys() throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = prepareKeysQuery(c)) {
            return executeKeysQuery(ps);
        } catch (SQLException e) {
            log.error("keys() failed", e);
            throw new IOException(e);
        }
    }

    @Override
    public void remove(String id) throws IOException {
        deleteById(id);
    }

    @Override
    public void clear() throws IOException {
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE " + SessionTableSql.ATTRS_TABLE + " CASCADE");
            s.execute("TRUNCATE TABLE " + SessionTableSql.SESSION_TABLE + " CASCADE");
        } catch (SQLException e) {
            log.error("clear() failed", e);
            throw new IOException(e);
        }
    }

    public void shutdown() {
        if (cleanupExec != null) {
            cleanupExec.shutdownNow();
        }
    }

    private void initializeCleanupInterval() {
        cleanupInterval = Integer.parseInt(
                ConfigurationManager.getInstance()
                        .getProperty("session.cleanup.interval", "300"));
    }

    private void createDatabaseTables() {
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.execute(SessionTableSql.CREATE_SESSION_TABLE);
            s.execute(SessionTableSql.CREATE_ATTRS_TABLE);
            s.execute(SessionTableSql.CREATE_INDEX_EXPIRE);
            s.execute(SessionTableSql.CREATE_INDEX_LAST_ACCESS);
            s.execute(SessionTableSql.CREATE_INDEX_CREATED_TIME);
            s.execute(SessionTableSql.CREATE_INDEX_ATTRS_SESSION_ID);
            s.execute(SessionTableSql.CREATE_INDEX_ATTRS_NAME);
        } catch (SQLException e) {
            log.error("Failed to create/verify session tables or indexes", e);
            throw new RuntimeException(e);
        }
    }

    private void startCleanupScheduler() {
        cleanupExec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JDBC-Session-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExec.scheduleAtFixedRate(this::cleanupExpired,
                cleanupInterval, cleanupInterval,
                TimeUnit.SECONDS);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM " + SessionTableSql.SESSION_TABLE +
                                " WHERE expire_time>0 AND expire_time<?")) {

            ps.setLong(1, now);
            int deleted = ps.executeUpdate();
            if (deleted > 0)
                log.info("Cleaned " + deleted + " expired sessions");
        } catch (SQLException e) {
            log.warn("Error cleaning sessions", e);
        }
    }

    private PreparedStatement prepareSizeQuery(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM " + SessionTableSql.SESSION_TABLE +
                        " WHERE expire_time<=0 OR expire_time>?");
        ps.setLong(1, System.currentTimeMillis());
        return ps;
    }

    private PreparedStatement prepareKeysQuery(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM " + SessionTableSql.SESSION_TABLE +
                        " WHERE expire_time<=0 OR expire_time>?");
        ps.setLong(1, System.currentTimeMillis());
        return ps;
    }

    private String[] executeKeysQuery(PreparedStatement ps) throws SQLException {
        List<String> ids = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                ids.add(rs.getString(1));
        }
        return ids.toArray(new String[0]);
    }

    private StandardSession createEmptySession(String id) {
        StandardSession session = (StandardSession) getManager().createEmptySession();
        session.setId(id, false);
        return session;
    }

    private void loadSessionMetadata(StandardSession session, String id) throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_SESSION)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || isSessionExpired(rs.getLong("expire_time"))) {
                    scheduleDelete(id);
                    throw new IOException("Session not found or expired: " + id);
                }
                populateSessionMetadata(rs, session);
            }
        } catch (SQLException e) {
            log.error("load() metadata failed for " + id, e);
            throw new IOException("Failed to load session metadata: " + id, e);
        }
    }

    private boolean isSessionExpired(long expire) {
        return expire > 0 && System.currentTimeMillis() > expire;
    }

    private void populateSessionMetadata(ResultSet rs, StandardSession session) throws SQLException {
        session.setValid(true);
        session.setMaxInactiveInterval(rs.getInt("max_inactive"));
        session.setCreationTime(rs.getLong("created_time"));

        session.setAttribute(io.github.t1willi.security.session.Session.KEY_INITIALIZED,
                true);
        session.setAttribute(io.github.t1willi.security.session.Session.KEY_LAST_ACCESS,
                rs.getLong("last_access"));
        session.setAttribute(io.github.t1willi.security.session.Session.KEY_EXPIRE_TIME,
                rs.getLong("expire_time"));
        // ────────────────────────────────────────────────────────────────────

        session.setAttribute(io.github.t1willi.security.session.Session.KEY_IP_ADDRESS,
                rs.getString("ip_address"));
        session.setAttribute(io.github.t1willi.security.session.Session.KEY_USER_AGENT,
                rs.getString("user_agent"));

        session.setManager(getManager());
    }

    private void loadSessionAttributes(StandardSession session, String id) throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_ATTRS)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (Constant.SessionKeys.EXCLUDED_KEY_FOR_SESSION_ATTRIBUTES.contains(name))
                        continue;
                    String json = rs.getString("value");
                    Object value = JacksonUtil.getObjectMapper().readValue(json, Object.class);
                    session.setAttribute(name, value);
                }
            }
        } catch (Exception e) {
            log.error("load() attributes failed for " + id, e);
            throw new IOException("Failed to load session attributes: " + id, e);
        }
    }

    private void saveSessionToDatabase(StandardSession session) throws IOException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            saveSessionMetadata(c, session);
            saveSessionAttributes(c, session);
            c.commit();
            log.debug("Session saved: " + session.getId());
        } catch (SQLException e) {
            log.error("save() failed for " + session.getId(), e);
            throw new IOException("Failed to save session: " + session.getId(), e);
        }
    }

    private void saveSessionMetadata(Connection c, StandardSession session) throws SQLException {
        long lastAccess = session.getLastAccessedTime();
        int maxInactive = session.getMaxInactiveInterval();
        long expire = maxInactive > 0
                ? lastAccess + (maxInactive * 1000L)
                : -1L;

        try (PreparedStatement ps = c.prepareStatement(SessionTableSql.UPSERT_SESSION)) {
            ps.setString(1, session.getId());
            ps.setLong(2, lastAccess);
            ps.setInt(3, maxInactive);
            ps.setLong(4, expire);
            ps.setString(5, (String) session.getAttribute(
                    io.github.t1willi.security.session.Session.KEY_IP_ADDRESS));
            ps.setString(6, (String) session.getAttribute(
                    io.github.t1willi.security.session.Session.KEY_USER_AGENT));
            ps.setLong(7, session.getCreationTime());
            ps.executeUpdate();
        }
    }

    private void saveSessionAttributes(Connection c, StandardSession session) throws SQLException, IOException {
        String sessionId = session.getId();
        Set<String> existing = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_ATTRS)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    existing.add(rs.getString("name"));
            }
        }

        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (Constant.SessionKeys.EXCLUDED_KEY_FOR_SESSION_ATTRIBUTES.contains(name))
                continue;
            String json = JacksonUtil.getObjectMapper().writeValueAsString(
                    session.getAttribute(name));
            if (existing.remove(name)) {
                try (PreparedStatement ps = c.prepareStatement(SessionTableSql.UPDATE_ATTR)) {
                    ps.setString(1, json);
                    ps.setString(2, sessionId);
                    ps.setString(3, name);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(SessionTableSql.INSERT_ATTR)) {
                    ps.setString(1, sessionId);
                    ps.setString(2, name);
                    ps.setString(3, json);
                    ps.executeUpdate();
                }
            }
        }
        for (String stale : existing) {
            try (PreparedStatement ps = c.prepareStatement(SessionTableSql.DELETE_ATTR)) {
                ps.setString(1, sessionId);
                ps.setString(2, stale);
                ps.executeUpdate();
            }
        }
    }

    private void deleteById(String id) throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(SessionTableSql.DELETE_SESSION)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("deleteById failed for " + id, e);
            throw new IOException(e);
        }
    }

    private void scheduleDelete(String id) {
        if (cleanupExec != null && !cleanupExec.isShutdown()) {
            cleanupExec.submit(() -> {
                try {
                    deleteById(id);
                } catch (IOException ex) {
                    log.warn("Scheduled delete failed for " + id, ex);
                }
            });
        }
    }
}
