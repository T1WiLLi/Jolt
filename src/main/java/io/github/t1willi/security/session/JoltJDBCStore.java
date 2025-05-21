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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;

public class JoltJDBCStore extends StoreBase {
    private static final Logger logger = Logger.getLogger(JoltJDBCStore.class.getName());
    private static final String DEFAULT_SESSION_TABLE = Constant.Security.DEFAULT_SESSION_TABLE_NAME;
    private String sessionTable;
    private Database database;

    @Override
    public void setManager(Manager manager) {
        super.setManager(manager);
        this.database = Database.getInstance();
        if (!database.isInitialized()) {
            logger.severe("Database not initialized; JoltJDBCStore cannot function.");
            throw new IllegalStateException("Database not initialized");
        }

        // Get table name from configuration
        this.sessionTable = ConfigurationManager.getInstance()
                .getProperty("session.table", DEFAULT_SESSION_TABLE);

        logger.info("Using session table: " + sessionTable);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "id VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "data BYTEA NOT NULL, " +
                        "app_name VARCHAR(255), " +
                        "last_access BIGINT NOT NULL, " +
                        "max_inactive INT NOT NULL, " +
                        "expiry_time BIGINT NOT NULL)",
                sessionTable);

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            logger.info("Session table '" + sessionTable + "' created or already exists.");
        } catch (SQLException e) {
            logger.severe("Failed to create session table: " + e.getMessage());
            throw new RuntimeException("Failed to initialize session persistence", e);
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
            logger.severe("Failed to get session count: " + e.getMessage());
            throw new IOException("Failed to get session count", e);
        }
    }

    @Override
    public String[] keys() throws IOException {
        String sql = "SELECT id FROM " + sessionTable;
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            var list = new ArrayList<String>();
            while (rs.next()) {
                list.add(rs.getString("id"));
            }
            return list.toArray(new String[0]);
        } catch (SQLException e) {
            logger.severe("Failed to get session keys: " + e.getMessage());
            throw new IOException("Failed to get session keys", e);
        }
    }

    @Override
    public Session load(String id) throws IOException {
        if (id == null || id.isEmpty()) {
            return null;
        }

        logger.fine("Loading session: " + id);
        String sql = "SELECT data, max_inactive, expiry_time FROM " + sessionTable + " WHERE id = ?";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    logger.fine("No session found with ID: " + id);
                    return null;
                }

                long expiryTime = rs.getLong("expiry_time");
                if (System.currentTimeMillis() > expiryTime) {
                    logger.fine("Session " + id + " has expired, removing it");
                    remove(id);
                    return null;
                }

                byte[] data = rs.getBytes("data");
                int maxInactive = rs.getInt("max_inactive");

                // Create a new session
                StandardSession session = (StandardSession) getManager().createEmptySession();
                session.setId(id, false); // Don't notify listeners yet
                session.setMaxInactiveInterval(maxInactive);

                try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        ObjectInputStream ois = new ObjectInputStream(bais)) {
                    session.readObjectData(ois);
                    session.setManager(getManager());

                    // Session is now valid - notify listeners
                    session.setId(id, true);

                    logger.fine("Loaded session " + id + " successfully");
                    return session;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load session " + id, e);
            throw new IOException("Failed to load session: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to deserialize session " + id, e);
            throw new IOException("Failed to deserialize session", e);
        }
    }

    @Override
    public void save(Session session) throws IOException {
        if (session == null || !(session instanceof StandardSession)) {
            return;
        }

        StandardSession standardSession = (StandardSession) session;
        if (!standardSession.isValid()) {
            logger.fine("Skip saving invalid session: " + session.getId());
            remove(session.getId());
            return;
        }

        logger.fine("Saving session: " + session.getId());

        String sql = String.format(
                "INSERT INTO %s (id, data, app_name, last_access, max_inactive, expiry_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET " +
                        "data = EXCLUDED.data, " +
                        "app_name = EXCLUDED.app_name, " +
                        "last_access = EXCLUDED.last_access, " +
                        "max_inactive = EXCLUDED.max_inactive, " +
                        "expiry_time = EXCLUDED.expiry_time",
                sessionTable);

        // Calculate expiry time correctly
        long lastAccessedTime = session.getLastAccessedTime();
        int maxInactiveInterval = session.getMaxInactiveInterval();
        long expiryTime = maxInactiveInterval > 0
                ? lastAccessedTime + (maxInactiveInterval * 1000L)
                : -1; // No expiry

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            standardSession.writeObjectData(oos);
            oos.flush();
            byte[] blob = baos.toByteArray();

            try (Connection conn = database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, session.getId());
                stmt.setBytes(2, blob);
                stmt.setString(3, session.getManager().getContext().getName());
                stmt.setLong(4, lastAccessedTime);
                stmt.setInt(5, maxInactiveInterval);
                stmt.setLong(6, expiryTime);

                int rowsUpdated = stmt.executeUpdate();
                logger.fine("Session " + session.getId() + " " +
                        (rowsUpdated > 0 ? "saved successfully" : "not saved"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save session " + session.getId(), e);
            throw new IOException("Failed to save session: " + e.getMessage(), e);
        }
    }

    @Override
    public void remove(String id) throws IOException {
        if (id == null || id.isEmpty()) {
            return;
        }

        logger.fine("Removing session: " + id);
        String sql = "DELETE FROM " + sessionTable + " WHERE id = ?";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            int rowsDeleted = stmt.executeUpdate();

            logger.fine("Session " + id + " " +
                    (rowsDeleted > 0 ? "removed successfully" : "not found"));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to remove session " + id, e);
            throw new IOException("Failed to remove session: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() throws IOException {
        logger.fine("Clearing all sessions");
        String sql = "DELETE FROM " + sessionTable;

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            int rowsDeleted = stmt.executeUpdate();
            logger.fine("Cleared " + rowsDeleted + " sessions");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear sessions", e);
            throw new IOException("Failed to clear sessions: " + e.getMessage(), e);
        }
    }
}