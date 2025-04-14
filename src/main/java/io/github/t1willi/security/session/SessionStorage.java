package io.github.t1willi.security.session;

import java.util.Map;

public interface SessionStorage {
    void saveSession(String sessionId, Map<String, Object> attributes);

    Map<String, Object> loadSession(String sessionId);

    void deleteSession(String sessionId);
}
