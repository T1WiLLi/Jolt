package io.github.t1willi.security.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public final class SessionInfo {
    private final String sessionId;
    private final String clientIp;
    private final String userAgent;
    private final String intrustionType;
    private final String expectedValue;
    private final String actualValue;
}
