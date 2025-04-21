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
import java.util.logging.Logger;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.config.ConfigurationManager;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;

public class JoltJDBCStore extends StoreBase {
    private static final Logger logger = Logger.getLogger(JoltJDBCStore.class.getName());
    private static final String DEFAULT_SESSION_TABLE = "tomcat_sessions";
    private static String sessionTable;
    private Database database;

    @Override
    public void setManager(Manager manager) {
        super.setManager(manager);
        this.database = Database.getInstance();
        if (!database.isInitialized()) {
            logger.severe("Database not initialized; CustomJDBCStore cannot function.");
            throw new IllegalStateException("Database not initialized");
        }
        sessionTable = ConfigurationManager.getInstance().getProperty("session.name=", DEFAULT_SESSION_TABLE);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(255) NOT NULL PRIMARY KEY,
                    data BYTEA NOT NULL,
                    app_name VARCHAR(255),
                    last_access BIGINT NOT NULL,
                    max_inactive INT NOT NULL,
                    expiry_time BIGINT NOT NULL
                )
                """.formatted(sessionTable);
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
            logger.info("Database table '" + sessionTable + "' created or already exists.");
        } catch (SQLException e) {
            logger.severe("Failed to create " + sessionTable + " table: " + e.getMessage());
            throw new RuntimeException("Failed to initialize session persistence", e);
        }
    }

    @Override
    public int getSize() throws IOException {
        String sql = "SELECT COUNT(*) FROM " + sessionTable;
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
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
            ArrayList<String> keys = new ArrayList<>();
            while (rs.next()) {
                keys.add(rs.getString("id"));
            }
            return keys.toArray(new String[0]);
        } catch (SQLException e) {
            logger.severe("Failed to get session keys: " + e.getMessage());
            throw new IOException("Failed to get session keys", e);
        }
    }

    @Override
    public Session load(String id) throws IOException {
        String sql = "SELECT data, max_inactive, expiry_time FROM " + sessionTable + " WHERE id = ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] data = rs.getBytes("data");
                    int maxInactive = rs.getInt("max_inactive");
                    long expiryTime = rs.getLong("expiry_time");

                    if (System.currentTimeMillis() > expiryTime) {
                        remove(id);
                        return null;
                    }

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                            ObjectInputStream ois = new ObjectInputStream(bais)) {
                        Session session = getManager().createEmptySession();
                        session.setId(id);
                        session.setMaxInactiveInterval(maxInactive);
                        ((StandardSession) session).readObjectData(ois);
                        return session;
                    }
                }
            }
            return null;
        } catch (SQLException | ClassNotFoundException e) {
            logger.severe("Failed to load session " + id + ": " + e.getMessage());
            throw new IOException("Failed to load session", e);
        }
    }

    @Override
    public void save(Session session) throws IOException {
        String sql = """
                INSERT INTO %s (id, data, app_name, last_access, max_inactive, expiry_time)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET data = EXCLUDED.data,
                    app_name = EXCLUDED.app_name,
                    last_access = EXCLUDED.last_access,
                    max_inactive = EXCLUDED.max_inactive,
                    expiry_time = EXCLUDED.expiry_time
                """.formatted(sessionTable);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            ((StandardSession) session).writeObjectData(oos);
            oos.flush();
            byte[] data = baos.toByteArray();

            try (Connection conn = database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, session.getId());
                stmt.setBytes(2, data);
                stmt.setString(3, session.getManager().getContext().getName());
                stmt.setLong(4, session.getLastAccessedTime());
                stmt.setInt(5, session.getMaxInactiveInterval());
                stmt.setLong(6, session.getLastAccessedTime() + (session.getMaxInactiveInterval() * 1000L));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Failed to save session " + session.getId() + ": " + e.getMessage());
            throw new IOException("Failed to save session", e);
        }
    }

    @Override
    public void remove(String id) throws IOException {
        String sql = "DELETE FROM " + sessionTable + " WHERE id = ?";
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to remove session " + id + ": " + e.getMessage());
            throw new IOException("Failed to remove session", e);
        }
    }

    @Override
    public void clear() throws IOException {
        String sql = "DELETE FROM " + sessionTable;
        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to clear sessions: " + e.getMessage());
            throw new IOException("Failed to clear sessions", e);
        }
    }
}