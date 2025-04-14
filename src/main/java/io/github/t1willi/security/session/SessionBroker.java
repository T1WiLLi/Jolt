package io.github.t1willi.security.session;

import io.github.t1willi.database.RestBroker;

public class SessionBroker extends RestBroker<String, SessionEntity> {

    public SessionBroker(String table) {
        super(table, SessionEntity.class, String.class, "session_id");
        initializeTable();
    }

    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + this.table + " (" +
                "session_id VARCHAR(255) PRIMARY KEY, " +
                "access INT NOT NULL, " +
                "data TEXT NOT NULL, " +
                "expire INT NOT NULL, " +
                "ip_address VARCHAR(25) NULL DEFAULT NULL, " +
                "user_agent TEXT NULL DEFAULT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT now())";
        query(sql);
    }

    public void updateAccessAndExpire(String sessionId, int access, int expire) {
        String sql = "UPDATE " + table + " SET access = ?, expire = ? WHERE session_id = ?";
        query(sql, access, expire, sessionId);
    }
}
