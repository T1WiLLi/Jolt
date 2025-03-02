package ca.jolt.server.config;

import java.util.Properties;

public class ServerConfig {
    // Default constants
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_TEMP_DIR = "tmp/tomcat";
    private static final boolean DEFAULT_SSL_ENABLED = false;
    private static final int DEFAULT_SSL_PORT = 8443;
    private static final String DEFAULT_KEYSTORE = "";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "";
    private static final String DEFAULT_KEY_ALIAS = "";
    private static final int DEFAULT_THREADS_MIN = 10;
    private static final int DEFAULT_THREADS_MAX = 200;
    private static final long DEFAULT_THREADS_TIMEOUT = 60000;
    private static final boolean DEFAULT_DAEMON = false;
    private static final String DEFAULT_APP_NAME = "JoltApp";

    // New multipart defaults
    private static final long DEFAULT_MULTIPART_MAX_FILE_SIZE = 15 * 1024L * 1024L; // 15MB
    private static final long DEFAULT_MULTIPART_MAX_REQUEST_SIZE = 50 * 1024L * 1024L; // 50MB
    private static final int DEFAULT_MULTIPART_FILE_SIZE_THRESHOLD = 1024 * 1024; // 1MB

    // Actual fields
    private final int port;
    private final String tempDir;
    private final boolean sslEnabled;
    private final int sslPort;
    private final String keyStore;
    private final String keyStorePassword;
    private final String keyAlias;
    private final int threadsMin;
    private final int threadsMax;
    private final long threadsTimeout;
    private final boolean daemon;
    private final String appName;
    private final long multipartMaxFileSize;
    private final long multipartMaxRequestSize;
    private final int multipartFileSizeThreshold;

    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.tempDir = builder.tempDir;
        this.sslEnabled = builder.sslEnabled;
        this.sslPort = builder.sslPort;
        this.keyStore = builder.keyStore;
        this.keyStorePassword = builder.keyStorePassword;
        this.keyAlias = builder.keyAlias;
        this.threadsMin = builder.threadsMin;
        this.threadsMax = builder.threadsMax;
        this.threadsTimeout = builder.threadsTimeout;
        this.daemon = builder.daemon;
        this.appName = builder.appName;
        this.multipartMaxFileSize = builder.multipartMaxFileSize;
        this.multipartMaxRequestSize = builder.multipartMaxRequestSize;
        this.multipartFileSizeThreshold = builder.multipartFileSizeThreshold;
    }

    // Getters...
    public int getPort() {
        return port;
    }

    public String getTempDir() {
        return tempDir;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public int getSslPort() {
        return sslPort;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public int getThreadsMin() {
        return threadsMin;
    }

    public int getThreadsMax() {
        return threadsMax;
    }

    public long getThreadsTimeout() {
        return threadsTimeout;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public String getAppName() {
        return appName;
    }

    public long getMultipartMaxFileSize() {
        return multipartMaxFileSize;
    }

    public long getMultipartMaxRequestSize() {
        return multipartMaxRequestSize;
    }

    public int getMultipartFileSizeThreshold() {
        return multipartFileSizeThreshold;
    }

    public static class Builder {
        private int port = DEFAULT_PORT;
        private String tempDir = DEFAULT_TEMP_DIR;
        private boolean sslEnabled = DEFAULT_SSL_ENABLED;
        private int sslPort = DEFAULT_SSL_PORT;
        private String keyStore = DEFAULT_KEYSTORE;
        private String keyStorePassword = DEFAULT_KEYSTORE_PASSWORD;
        private String keyAlias = DEFAULT_KEY_ALIAS;
        private int threadsMin = DEFAULT_THREADS_MIN;
        private int threadsMax = DEFAULT_THREADS_MAX;
        private long threadsTimeout = DEFAULT_THREADS_TIMEOUT;
        private boolean daemon = DEFAULT_DAEMON;
        private String appName = DEFAULT_APP_NAME;
        private long multipartMaxFileSize = DEFAULT_MULTIPART_MAX_FILE_SIZE;
        private long multipartMaxRequestSize = DEFAULT_MULTIPART_MAX_REQUEST_SIZE;
        private int multipartFileSizeThreshold = DEFAULT_MULTIPART_FILE_SIZE_THRESHOLD;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder tempDir(String tempDir) {
            this.tempDir = tempDir;
            return this;
        }

        public Builder sslEnabled(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
            return this;
        }

        public Builder sslPort(int sslPort) {
            this.sslPort = sslPort;
            return this;
        }

        public Builder keyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public Builder keyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
            return this;
        }

        public Builder keyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
            return this;
        }

        public Builder threadsMin(int threadsMin) {
            this.threadsMin = threadsMin;
            return this;
        }

        public Builder threadsMax(int threadsMax) {
            this.threadsMax = threadsMax;
            return this;
        }

        public Builder threadsTimeout(long threadsTimeout) {
            this.threadsTimeout = threadsTimeout;
            return this;
        }

        public Builder daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder multipartMaxFileSize(long size) {
            this.multipartMaxFileSize = size;
            return this;
        }

        public Builder multipartMaxRequestSize(long size) {
            this.multipartMaxRequestSize = size;
            return this;
        }

        public Builder multipartFileSizeThreshold(int threshold) {
            this.multipartFileSizeThreshold = threshold;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }

    public static ServerConfig fromProperties(Properties props) {
        Builder builder = new Builder();
        builder.port(Integer.parseInt(props.getProperty("server.port", String.valueOf(DEFAULT_PORT))));
        builder.tempDir(props.getProperty("server.tempDir", DEFAULT_TEMP_DIR));
        builder.sslEnabled(
                Boolean.parseBoolean(props.getProperty("server.ssl.enabled", String.valueOf(DEFAULT_SSL_ENABLED))));
        builder.sslPort(Integer.parseInt(props.getProperty("server.ssl.port", String.valueOf(DEFAULT_SSL_PORT))));
        builder.keyStore(props.getProperty("server.ssl.keyStore", DEFAULT_KEYSTORE));
        builder.keyStorePassword(props.getProperty("server.ssl.keyStorePassword", DEFAULT_KEYSTORE_PASSWORD));
        builder.keyAlias(props.getProperty("server.ssl.keyAlias", DEFAULT_KEY_ALIAS));
        builder.threadsMin(
                Integer.parseInt(props.getProperty("server.threads.min", String.valueOf(DEFAULT_THREADS_MIN))));
        builder.threadsMax(
                Integer.parseInt(props.getProperty("server.threads.max", String.valueOf(DEFAULT_THREADS_MAX))));
        builder.threadsTimeout(
                Long.parseLong(props.getProperty("server.threads.timeout", String.valueOf(DEFAULT_THREADS_TIMEOUT))));
        builder.daemon(Boolean.parseBoolean(props.getProperty("server.daemon", String.valueOf(DEFAULT_DAEMON))));
        builder.appName(props.getProperty("server.appName", DEFAULT_APP_NAME));
        builder.multipartMaxFileSize(Long.parseLong(
                props.getProperty("server.multipart.maxFileSize", String.valueOf(DEFAULT_MULTIPART_MAX_FILE_SIZE))));
        builder.multipartMaxRequestSize(Long.parseLong(props.getProperty("server.multipart.maxRequestSize",
                String.valueOf(DEFAULT_MULTIPART_MAX_REQUEST_SIZE))));
        builder.multipartFileSizeThreshold(Integer.parseInt(props.getProperty("server.multipart.fileSizeThreshold",
                String.valueOf(DEFAULT_MULTIPART_FILE_SIZE_THRESHOLD))));
        return builder.build();
    }
}