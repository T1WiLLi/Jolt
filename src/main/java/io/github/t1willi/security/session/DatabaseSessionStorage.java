package io.github.t1willi.security.session;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.database.exception.DatabaseErrorType;
import io.github.t1willi.database.exception.DatabaseException;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.utils.JacksonUtil;

class DatabaseSessionStorage implements SessionStorage {
    private final SessionBroker sessionBroker;
    private final ObjectMapper objectMapper = JacksonUtil.getObjectMapper();
    private final SessionConfig config;

    public DatabaseSessionStorage(SessionConfig config) {
        this.config = config;
        this.sessionBroker = new SessionBroker(this.config.getSessionTableName());
    }

    @Override
    public void saveSession(String sessionId, Map<String, Object> attributes) {
        try {
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            int expireTime = currentTime + config.getSessionLifetime();

            JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
            String ipAddress = ctx.clientIp();
            String userAgent = this.objectMapper.writeValueAsString(ctx.userAgent());
            String dataJson = this.objectMapper.writeValueAsString(attributes);
            SessionEntity entity = new SessionEntity(sessionId, currentTime, expireTime, dataJson, ipAddress,
                    userAgent);
            this.sessionBroker.save(entity);
        } catch (Exception e) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR, "Failed to save session to database",
                    e.getMessage(), e.getCause());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadSession(String sessionId) {
        Optional<SessionEntity> entityOpt = sessionBroker.findById(sessionId);
        if (entityOpt.isPresent()) {
            SessionEntity entity = entityOpt.get();
            int currentTime = (int) (System.currentTimeMillis() / 1000);

            if (currentTime > entity.getExpire()) {
                this.sessionBroker.deleteById(sessionId);
                return null;
            }

            int newExpireTime = currentTime + config.getSessionLifetime();
            this.sessionBroker.updateAccessAndExpire(sessionId, currentTime, newExpireTime);

            try {
                String dataJson = entity.getData();
                return this.objectMapper.readValue(dataJson, Map.class);
            } catch (Exception e) {
                throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR,
                        "Failed to save session to database", e.getMessage(), e.getCause());
            }
        }
        return null;
    }

    @Override
    public void deleteSession(String sessionID) {
        sessionBroker.deleteById(sessionID);
    }
}
