package io.github.t1willi.security.session;

import lombok.Getter;

@Getter
public abstract class SessionConfig {
    private String sessionTableName = "jolt_sessions";
    private StorageType storageType = StorageType.DATABASE;
    private String fileStoragePath = "../tmp/tomcat";
    private int sessionLifetime = 1800; // 30 minutes (1800 seconds)

    protected abstract void configure();

    public SessionConfig withSessionTableName(String n) {
        this.sessionTableName = n;
        return this;
    }

    public SessionConfig withStorageType(StorageType t) {
        this.storageType = t;
        return this;
    }

    public SessionConfig withFileStoragePath(String p) {
        this.fileStoragePath = p;
        return this;
    }

    public SessionConfig withSessionLifetime(int l) { // Where l is in seconds
        if (l < 0) {
            throw new IllegalArgumentException("Session lifetime must be a non-negative integer !");
        }
        this.sessionLifetime = l;
        return this;
    }

    public static enum StorageType {
        DATABASE,
        FILE;
    }
}
