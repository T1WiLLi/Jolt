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
        logInitialization();
    }

    @Override
    public void processExpires() {
        cleanupExpired();
    }

    @Override
    public int getSize() throws IOException {
        try (Connection c = db.getConnection();
                PreparedStatement ps = prepareSizeQuery(c)) {
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
        try (Connection c = db.getConnection();
                PreparedStatement ps = prepareKeysQuery(c)) {
            return executeKeysQuery(ps);
        } catch (SQLException e) {
            log.error("keys() failed", e);
            throw new IOException(e);
        }
    }

    @Override
    public Session load(String id) throws IOException {
        if (isInvalidId(id))
            return null;

        StandardSession session = createEmptySession(id);
        loadSessionMetadata(session, id);
        loadSessionAttributes(session, id);
        return session;
    }

    @Override
    public void save(Session session) throws IOException {
        if (!isValidStandardSession(session))
            return;

        StandardSession ss = (StandardSession) session;
        if (!ss.isValid()) {
            deleteById(ss.getId());
            return;
        }
        saveSessionToDatabase(ss);
    }

    @Override
    public void remove(String id) throws IOException {
        deleteById(id);
    }

    @Override
    public void clear() throws IOException {
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            clearAllTables(s);
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
            createTables(s);
            createIndexes(s);
        } catch (SQLException e) {
            log.error("Failed to create/verify session tables or indexes", e);
            throw new RuntimeException(e);
        }
    }

    private void createTables(Statement statement) throws SQLException {
        statement.execute(SessionTableSql.CREATE_SESSION_TABLE);
        statement.execute(SessionTableSql.CREATE_ATTRS_TABLE);
    }

    private void createIndexes(Statement statement) throws SQLException {
        statement.execute(SessionTableSql.CREATE_INDEX_EXPIRE);
        statement.execute(SessionTableSql.CREATE_INDEX_LAST_ACCESS);
        statement.execute(SessionTableSql.CREATE_INDEX_CREATED_TIME);
        statement.execute(SessionTableSql.CREATE_INDEX_ATTRS_SESSION_ID);
        statement.execute(SessionTableSql.CREATE_INDEX_ATTRS_NAME);
    }

    private void startCleanupScheduler() {
        cleanupExec = createCleanupExecutor();
        cleanupExec.scheduleAtFixedRate(
                this::cleanupExpired,
                cleanupInterval, cleanupInterval,
                TimeUnit.SECONDS);
    }

    private ScheduledExecutorService createCleanupExecutor() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JDBC-Session-Cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    private void logInitialization() {
        log.info("JDBC session store initialized (cleanup every "
                + cleanupInterval + "s)");
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        try (Connection c = db.getConnection();
                PreparedStatement ps = prepareCleanupStatement(c, now)) {

            int deleted = ps.executeUpdate();
            logCleanupResults(deleted);
        } catch (SQLException e) {
            log.warn("Error cleaning sessions", e);
        }
    }

    private PreparedStatement prepareCleanupStatement(Connection c, long now) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + SessionTableSql.SESSION_TABLE +
                        " WHERE expire_time>0 AND expire_time<?");
        ps.setLong(1, now);
        return ps;
    }

    private void logCleanupResults(int deleted) {
        if (deleted > 0) {
            log.info("Cleaned " + deleted + " expired sessions");
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
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        }
        return ids.toArray(new String[0]);
    }

    private boolean isInvalidId(String id) {
        return id == null || id.isEmpty();
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
                if (!processSessionMetadata(rs, session, id)) {
                    throw new IOException("Session not found or expired: " + id);
                }
            }
        } catch (SQLException e) {
            log.error("load() metadata failed for " + id, e);
            throw new IOException("Failed to load session metadata: " + id, e);
        }
    }

    private boolean processSessionMetadata(ResultSet rs, StandardSession session, String id)
            throws SQLException, IOException {
        if (!rs.next())
            return false;

        long expire = rs.getLong("expire_time");
        if (isSessionExpired(expire)) {
            scheduleDelete(id);
            return false;
        }

        populateSessionMetadata(rs, session);
        return true;
    }

    private boolean isSessionExpired(long expire) {
        return expire > 0 && System.currentTimeMillis() > expire;
    }

    private void populateSessionMetadata(ResultSet rs, StandardSession session) throws SQLException {
        session.setValid(true);
        session.setMaxInactiveInterval(rs.getInt("max_inactive"));
        session.setCreationTime(rs.getLong("created_time"));
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
                processSessionAttributes(rs, session);
            }
        } catch (Exception e) {
            log.error("load() attributes failed for " + id, e);
            throw new IOException("Failed to load session attributes: " + id, e);
        }
    }

    private void processSessionAttributes(ResultSet rs, StandardSession session) throws Exception {
        while (rs.next()) {
            String name = rs.getString("name");
            if (shouldSkipAttribute(name))
                continue;

            String json = rs.getString("value");
            Object value = deserializeAttribute(json);
            session.setAttribute(name, value);
        }
    }

    private boolean shouldSkipAttribute(String name) {
        return Constant.SessionKeys.EXCLUDED_KEY_FOR_SESSION_ATTRIBUTES.contains(name);
    }

    private Object deserializeAttribute(String json) throws Exception {
        return JacksonUtil.getObjectMapper().readValue(json, Object.class);
    }

    private boolean isValidStandardSession(Session session) {
        return session instanceof StandardSession;
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
        long expire = calculateexpireTime(lastAccess, maxInactive);

        try (PreparedStatement ps = c.prepareStatement(SessionTableSql.UPSERT_SESSION)) {
            populateSessionInsertStatement(ps, session, lastAccess, maxInactive, expire);
            ps.executeUpdate();
        }
    }

    private long calculateexpireTime(long lastAccess, int maxInactive) {
        return maxInactive > 0 ? lastAccess + (maxInactive * 1000L) : -1L;
    }

    private void populateSessionInsertStatement(PreparedStatement ps, StandardSession session,
            long lastAccess, int maxInactive, long expire) throws SQLException {
        ps.setString(1, session.getId());
        ps.setLong(2, lastAccess);
        ps.setInt(3, maxInactive);
        ps.setLong(4, expire);
        ps.setString(5, (String) session.getAttribute(io.github.t1willi.security.session.Session.KEY_IP_ADDRESS));
        ps.setString(6, (String) session.getAttribute(io.github.t1willi.security.session.Session.KEY_USER_AGENT));
        ps.setLong(7, session.getCreationTime());
    }

    private void saveSessionAttributes(Connection c, StandardSession session) throws SQLException {
        String id = session.getId();
        Set<String> existingAttrs = getExistingAttributeNames(c, id);
        processAttributeUpdates(c, session, existingAttrs);
        removeDeletedAttributes(c, id, existingAttrs);
    }

    private Set<String> getExistingAttributeNames(Connection c, String id) throws SQLException {
        Set<String> existing = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(SessionTableSql.SELECT_ATTRS)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getString("name"));
                }
            }
        }
        return existing;
    }

    private void processAttributeUpdates(Connection c, StandardSession session, Set<String> existing)
            throws SQLException {
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (shouldSkipAttribute(name))
                continue;

            String json = serializeAttribute(session.getAttribute(name));
            updateOrInsertAttribute(c, session.getId(), name, json, existing);
        }
    }

    private String serializeAttribute(Object attribute) throws SQLException {
        try {
            return JacksonUtil.getObjectMapper().writeValueAsString(attribute);
        } catch (Exception e) {
            throw new SQLException("Failed to serialize attribute", e);
        }
    }

    private void updateOrInsertAttribute(Connection c, String sessionId, String name,
            String json, Set<String> existing) throws SQLException {
        if (existing.remove(name)) {
            updateExistingAttribute(c, sessionId, name, json);
        } else {
            insertNewAttribute(c, sessionId, name, json);
        }
    }

    private void updateExistingAttribute(Connection c, String sessionId, String name, String json) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SessionTableSql.UPDATE_ATTR)) {
            ps.setString(1, json);
            ps.setString(2, sessionId);
            ps.setString(3, name);
            ps.executeUpdate();
        }
    }

    private void insertNewAttribute(Connection c, String sessionId, String name, String json) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SessionTableSql.INSERT_ATTR)) {
            ps.setString(1, sessionId);
            ps.setString(2, name);
            ps.setString(3, json);
            ps.executeUpdate();
        }
    }

    private void removeDeletedAttributes(Connection c, String sessionId, Set<String> deletedAttrs) throws SQLException {
        for (String name : deletedAttrs) {
            try (PreparedStatement ps = c.prepareStatement(SessionTableSql.DELETE_ATTR)) {
                ps.setString(1, sessionId);
                ps.setString(2, name);
                ps.executeUpdate();
            }
        }
    }

    private void clearAllTables(Statement statement) throws SQLException {
        statement.execute("TRUNCATE TABLE " + SessionTableSql.ATTRS_TABLE + " CASCADE");
        statement.execute("TRUNCATE TABLE " + SessionTableSql.SESSION_TABLE + " CASCADE");
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