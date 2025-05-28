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

        cleanupInterval = Integer.parseInt(
                ConfigurationManager.getInstance()
                        .getProperty("session.cleanup.interval", "300"));

        try (Connection c = db.getConnection();
                Statement s = c.createStatement()) {

            s.execute(SessionTableSql.CREATE_SESSION_TABLE);
            s.execute(SessionTableSql.CREATE_ATTRS_TABLE);

            s.execute(SessionTableSql.CREATE_INDEX_EXPIRY);
            s.execute(SessionTableSql.CREATE_INDEX_LAST_ACCESS);
            s.execute(SessionTableSql.CREATE_INDEX_CREATED_TIME);
            s.execute(SessionTableSql.CREATE_INDEX_ATTRS_SESSION_ID);
            s.execute(SessionTableSql.CREATE_INDEX_ATTRS_NAME);
        } catch (SQLException e) {
            log.error("Failed to create/verify session tables or indexes", e);
            throw new RuntimeException(e);
        }

        cleanupExec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JDBC-Session-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExec.scheduleAtFixedRate(
                this::cleanupExpired,
                cleanupInterval, cleanupInterval,
                TimeUnit.SECONDS);

        log.info("JDBC session store initialized (cleanup every "
                + cleanupInterval + "s)");
    }

    @Override
    public void processExpires() {
        cleanupExpired();
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM " + SessionTableSql.SESSION_TABLE +
                                " WHERE expiry_time>0 AND expiry_time<?")) {

            ps.setLong(1, now);
            int deleted = ps.executeUpdate();
            if (deleted > 0)
                log.info("Cleaned " + deleted + " expired sessions");
        } catch (SQLException e) {
            log.warn("Error cleaning sessions", e);
        }
    }

    @Override
    public int getSize() throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM " + SessionTableSql.SESSION_TABLE +
                                " WHERE expiry_time<=0 OR expiry_time>?")) {

            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log.error("getSize() failed", e);
            throw new IOException(e);
        }
    }

    @Override
    public String[] keys() throws IOException {
        List<String> ids = new ArrayList<>();
        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM " + SessionTableSql.SESSION_TABLE +
                                " WHERE expiry_time<=0 OR expiry_time>?")) {

            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    ids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            log.error("keys() failed", e);
            throw new IOException(e);
        }
        return ids.toArray(new String[0]);
    }

    @Override
    public Session load(String id) throws IOException {
        if (id == null || id.isEmpty())
            return null;

        StandardSession sess = (StandardSession) getManager().createEmptySession();
        sess.setId(id, false);

        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_SESSION)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long expiry = rs.getLong("expiry_time");
                if (expiry > 0 && System.currentTimeMillis() > expiry) {
                    scheduleDelete(id);
                    return null;
                }
                sess.setMaxInactiveInterval(rs.getInt("max_inactive"));
                sess.setCreationTime(rs.getLong("created_time"));
                sess.setAttribute(io.github.t1willi.security.session.Session.KEY_IP_ADDRESS,
                        rs.getString("ip_address"));
                sess.setAttribute(io.github.t1willi.security.session.Session.KEY_USER_AGENT,
                        rs.getString("user_agent"));
            }
        } catch (SQLException e) {
            log.error("load() metadata failed for " + id, e);
            throw new IOException("Failed to load session metadata: " + id, e);
        }

        sess.setManager(getManager());

        try (Connection c = db.getConnection();
                PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_ATTRS)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (Constant.SessionKeys.EXCLUDED_KEY_FOR_SESSION_ATTRIBUTES.contains(name)) {
                        continue;
                    }
                    String json = rs.getString("value");
                    Object value = JacksonUtil.getObjectMapper().readValue(json, Object.class);
                    sess.setAttribute(name, value);
                }
            }
        } catch (Exception e) {
            log.error("load() attributes failed for " + id, e);
            throw new IOException("Failed to load session attributes: " + id, e);
        }

        return sess;
    }

    @Override
    public void save(Session session) throws IOException {
        if (!(session instanceof StandardSession ss))
            return;
        String id = ss.getId();

        if (!ss.isValid()) {
            deleteById(id);
            return;
        }

        long lastAccess = ss.getLastAccessedTime();
        int maxInactive = ss.getMaxInactiveInterval();
        long expiry = maxInactive > 0
                ? lastAccess + (maxInactive * 1000L)
                : -1L;

        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ups = c.prepareStatement(SessionTableSql.UPSERT_SESSION)) {
                ups.setString(1, id);
                ups.setLong(2, lastAccess);
                ups.setInt(3, maxInactive);
                ups.setLong(4, expiry);
                ups.setString(5, (String) ss.getAttribute(io.github.t1willi.security.session.Session.KEY_IP_ADDRESS));
                ups.setString(6, (String) ss.getAttribute(io.github.t1willi.security.session.Session.KEY_USER_AGENT));
                ups.setLong(7, ss.getCreationTime());
                ups.executeUpdate();
            }

            Set<String> existing = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_ATTRS)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        existing.add(rs.getString("name"));
                }
            }

            Enumeration<String> names = ss.getAttributeNames();
            while (names.hasMoreElements()) {
                String nm = names.nextElement();
                if (Constant.SessionKeys.EXCLUDED_KEY_FOR_SESSION_ATTRIBUTES.contains(nm)) {
                    continue;
                }
                String json = JacksonUtil.getObjectMapper().writeValueAsString(ss.getAttribute(nm));

                if (existing.remove(nm)) {
                    try (PreparedStatement up = c.prepareStatement(SessionTableSql.UPDATE_ATTR)) {
                        up.setString(1, json);
                        up.setString(2, id);
                        up.setString(3, nm);
                        up.executeUpdate();
                    }
                } else {
                    try (PreparedStatement in = c.prepareStatement(SessionTableSql.INSERT_ATTR)) {
                        in.setString(1, id);
                        in.setString(2, nm);
                        in.setString(3, json);
                        in.executeUpdate();
                    }
                }
            }

            for (String gone : existing) {
                try (PreparedStatement del = c.prepareStatement(SessionTableSql.DELETE_ATTR)) {
                    del.setString(1, id);
                    del.setString(2, gone);
                    del.executeUpdate();
                }
            }

            c.commit();
            log.debug("Session saved: " + id);

        } catch (SQLException e) {
            log.error("save() failed for " + id, e);
            throw new IOException("Failed to save session: " + id, e);
        }
    }

    @Override
    public void remove(String id) throws IOException {
        deleteById(id);
    }

    @Override
    public void clear() throws IOException {
        try (Connection c = db.getConnection();
                Statement s = c.createStatement()) {

            s.execute("TRUNCATE TABLE " + SessionTableSql.ATTRS_TABLE + " CASCADE");
            s.execute("TRUNCATE TABLE " + SessionTableSql.SESSION_TABLE + " CASCADE");
        } catch (SQLException e) {
            log.error("clear() failed", e);
            throw new IOException(e);
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

    public void shutdown() {
        if (cleanupExec != null) {
            cleanupExec.shutdownNow();
        }
    }
}
