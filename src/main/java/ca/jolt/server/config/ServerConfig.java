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

    private ServerConfig(
            int port,
            String tempDir,
            boolean sslEnabled,
            int sslPort,
            String keyStore,
            String keyStorePassword,
            String keyAlias,
            int threadsMin,
            int threadsMax,
            long threadsTimeout,
            boolean daemon,
            String appName) {
        this.port = port;
        this.tempDir = tempDir;
        this.sslEnabled = sslEnabled;
        this.sslPort = sslPort;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyAlias = keyAlias;
        this.threadsMin = threadsMin;
        this.threadsMax = threadsMax;
        this.threadsTimeout = threadsTimeout;
        this.daemon = daemon;
        this.appName = appName;
    }

    public static ServerConfig fromProperties(Properties props) {
        int port = Integer.parseInt(props.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        String tempDir = props.getProperty("server.tempDir", DEFAULT_TEMP_DIR);

        boolean sslEnabled = Boolean
                .parseBoolean(props.getProperty("server.ssl.enabled", String.valueOf(DEFAULT_SSL_ENABLED)));
        int sslPort = Integer.parseInt(props.getProperty("server.ssl.port", String.valueOf(DEFAULT_SSL_PORT)));
        String keyStore = props.getProperty("server.ssl.keyStore", DEFAULT_KEYSTORE);
        String keyStorePassword = props.getProperty("server.ssl.keyStorePassword", DEFAULT_KEYSTORE_PASSWORD);
        String keyAlias = props.getProperty("server.ssl.keyAlias", DEFAULT_KEY_ALIAS);

        int threadsMin = Integer.parseInt(props.getProperty("server.threads.min", String.valueOf(DEFAULT_THREADS_MIN)));
        int threadsMax = Integer.parseInt(props.getProperty("server.threads.max", String.valueOf(DEFAULT_THREADS_MAX)));
        long threadsTimeout = Long
                .parseLong(props.getProperty("server.threads.timeout", String.valueOf(DEFAULT_THREADS_TIMEOUT)));

        boolean daemon = Boolean.parseBoolean(props.getProperty("server.daemon", String.valueOf(DEFAULT_DAEMON)));
        String appName = props.getProperty("server.appName", DEFAULT_APP_NAME);

        return new ServerConfig(
                port, tempDir, sslEnabled, sslPort, keyStore, keyStorePassword, keyAlias,
                threadsMin, threadsMax, threadsTimeout, daemon, appName);
    }

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
}