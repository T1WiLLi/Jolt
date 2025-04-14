package io.github.t1willi.security.session;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.database.exception.DatabaseErrorType;
import io.github.t1willi.database.exception.DatabaseException;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.utils.JacksonUtil;

class FileSessionStorage implements SessionStorage {
    private final String storagePath;
    private final ObjectMapper objectMapper = JacksonUtil.getObjectMapper();
    private final SessionConfig config;

    public FileSessionStorage(SessionConfig config) {
        this.config = config;
        this.storagePath = config.getFileStoragePath();
        new File(this.storagePath).mkdirs();
    }

    @Override
    public void saveSession(String sessionId, Map<String, Object> attributes) {
        File file = new File(this.storagePath, sessionId + ".session");
        try (FileWriter writer = new FileWriter(file)) {
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            int expireTime = currentTime + config.getSessionLifetime();

            JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
            String ipAddress = ctx.clientIp();
            String userAgent = this.objectMapper.writeValueAsString(ctx.userAgent());
            String dataJson = this.objectMapper.writeValueAsString(attributes);

            this.objectMapper.writeValue(writer, Map.of(
                    "sessionId", sessionId,
                    "access", currentTime,
                    "expire", expireTime,
                    "data", dataJson,
                    "ipAddress", ipAddress,
                    "userAgent", userAgent,
                    "createdAt", LocalDateTime.now().toString()));
        } catch (IOException e) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR, "Failed to save session to file.",
                    e.getMessage(), e.getCause());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadSession(String sessionId) {
        File file = new File(this.storagePath, sessionId + ".session");
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            Map<String, Object> sessionData = this.objectMapper.readValue(reader, Map.class);
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            int expireTime = ((Number) sessionData.get("expire")).intValue();

            if (currentTime > expireTime) {
                deleteSession(sessionId);
                return null;
            }

            sessionData.put("access", currentTime);
            sessionData.put("expire", currentTime + config.getSessionLifetime());

            try (FileWriter writer = new FileWriter(file)) {
                this.objectMapper.writeValue(writer, sessionData);
            }

            String dataJson = (String) sessionData.get("data");
            return this.objectMapper.readValue(dataJson, Map.class);
        } catch (IOException e) {
            throw new DatabaseException(DatabaseErrorType.DATA_INTEGRITY_ERROR, "Failed to load session from file.",
                    e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        File file = new File(storagePath, sessionId + ".session");
        if (file.exists()) {
            file.delete();
        }
    }
}
