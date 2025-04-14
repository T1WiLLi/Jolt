package io.github.t1willi.security.session;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
class SessionEntity {
    private String sessionId;
    private int access;
    private int expire;
    private String data; // JSON string
    private String ipAddress;
    private String userAgent; // JSON string
    private final LocalDateTime createdAt = LocalDateTime.now();
}
