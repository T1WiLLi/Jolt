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

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;

/**
 * JDBC-backed Store for Tomcat sessions with expiration-aware removal and
 * startup cleanup of expired sessions to support stateless load-balanced
 * environments.
 */
public class JoltJDBCStore extends StoreBase {
    private static final String DEFAULT_TABLE = Constant.Security.DEFAULT_SESSION_TABLE_NAME;
    private String sessionTable;
    private Database database;

    @Override
    public void setManager(Manager manager) {
        super.setManager(manager);
        this.database = Database.getInstance();
        if (!database.isInitialized()) {
            throw new IllegalStateException("Database not initialized");
        }
        this.sessionTable = ConfigurationManager.getInstance()
                .getProperty("session.table", DEFAULT_TABLE);
        createTableIfNotExists();
        createIndexesIfNotExists();
        cleanupExpiredSessions();
    }

    private void createTableIfNotExists() {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s ("
                        + "id VARCHAR(255) PRIMARY KEY, data BYTEA NOT NULL,"
                        + " app_name VARCHAR(255), last_access BIGINT,"
                        + " max_inactive INT, expiry_time BIGINT,"
                        + " ip_address VARCHAR(255), user_agent TEXT)",
                sessionTable);
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize session table", e);
        }
    }

    private void createIndexesIfNotExists() {
        String idxExpiry = String.format(
                "CREATE INDEX IF NOT EXISTS idx_%1$s_expiry ON %1$s(expiry_time)",
                sessionTable);
        String idxLast = String.format(
                "CREATE INDEX IF NOT EXISTS idx_%1$s_last_access ON %1$s(last_access)",
                sessionTable);
        try (Connection conn = database.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(idxExpiry);
            stmt.executeUpdate(idxLast);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create session indexes", e);
        }
    }

    /**
     * Removes rows whose expiry_time has passed.
     */
    private void cleanupExpiredSessions() {
        String sql = "DELETE FROM " + sessionTable + " WHERE expiry_time > 0 AND expiry_time < ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cleanup expired sessions", e);
        }
    }

    @Override
    public int getSize() throws IOException {
        String sql = "SELECT COUNT(*) FROM " + sessionTable;
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IOException("Failed to get session count", e);
        }
    }

    @Override
    public String[] keys() throws IOException {
        String sql = "SELECT id FROM " + sessionTable;
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            ArrayList<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("id"));
            }
            return list.toArray(new String[0]);
        } catch (SQLException e) {
            throw new IOException("Failed to get session keys", e);
        }
    }

    @Override
    public Session load(String id) throws IOException {
        if (id == null || id.isEmpty())
            return null;
        String sql = "SELECT data, max_inactive, expiry_time FROM " + sessionTable + " WHERE id = ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next())
                    return null;
                long expiry = rs.getLong("expiry_time");
                if (expiry > 0 && System.currentTimeMillis() > expiry)
                    return null;
                byte[] blob = getDataBlob(id);
                StandardSession session = (StandardSession) getManager().createEmptySession();
                session.setId(id, false);
                session.setMaxInactiveInterval(rs.getInt("max_inactive"));
                try (ByteArrayInputStream bais = new ByteArrayInputStream(blob);
                        ObjectInputStream ois = new ObjectInputStream(bais)) {
                    session.readObjectData(ois);
                }
                session.setManager(getManager());
                session.setId(id, true);
                return session;
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new IOException("Failed to load session", e);
        }
    }

    // helper to fetch data blob separately
    private byte[] getDataBlob(String id) throws IOException {
        String sql = "SELECT data FROM " + sessionTable + " WHERE id = ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getBytes("data");
            }
        } catch (SQLException e) {
            throw new IOException("Failed to read session data", e);
        }
        return new byte[0];
    }

    @Override
    public void save(Session sess) throws IOException {
        if (!(sess instanceof StandardSession))
            return;
        StandardSession s = (StandardSession) sess;
        if (!s.isValid()) {
            deleteById(s.getId());
            return;
        }
        byte[] blob;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            s.writeObjectData(oos);
            oos.flush();
            blob = baos.toByteArray();
        }
        String sql = String.format(
                "INSERT INTO %s (id, data, app_name, last_access, max_inactive, expiry_time, ip_address, user_agent) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data, max_inactive = EXCLUDED.max_inactive, " +
                        "expiry_time = EXCLUDED.expiry_time, last_access = EXCLUDED.last_access",
                sessionTable);
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, s.getId());
            stmt.setBytes(2, blob);
            stmt.setString(3, s.getManager().getContext().getName());
            stmt.setLong(4, s.getLastAccessedTime());
            stmt.setInt(5, s.getMaxInactiveInterval());
            long expiry = s.getMaxInactiveInterval() > 0
                    ? s.getLastAccessedTime() + (s.getMaxInactiveInterval() * 1000L)
                    : -1;
            stmt.setLong(6, expiry);
            stmt.setString(7, (String) s.getAttribute(io.github.t1willi.security.session.Session.KEY_IP_ADDRESS));
            stmt.setString(8, (String) s.getAttribute(io.github.t1willi.security.session.Session.KEY_USER_AGENT));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to save session", e);
        }
    }

    /**
     * Deletes a session row by ID unconditionally.
     */
    private void deleteById(String id) throws IOException {
        String sql = "DELETE FROM " + sessionTable + " WHERE id = ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to delete session", e);
        }
    }

    @Override
    public void remove(String id) throws IOException {
        if (id == null || id.isEmpty())
            return;
        // only remove if expired
        String sql = "SELECT expiry_time FROM " + sessionTable + " WHERE id = ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || rs.getLong("expiry_time") > System.currentTimeMillis()) {
                    return;
                }
            }
        } catch (SQLException e) {
            // ignore, proceed to delete
        }
        deleteById(id);
    }

    @Override
    public void clear() throws IOException {
        String sql = "DELETE FROM " + sessionTable;
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to clear sessions", e);
        }
    }
}