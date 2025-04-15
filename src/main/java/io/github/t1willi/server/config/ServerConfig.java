package io.github.t1willi.server.config;

import java.util.Properties;

/**
 * Represents the configuration settings for the server.
 * <p>
 * This class encapsulates server-related settings such as port numbers, SSL
 * configuration,
 * thread pool settings, application name, and multipart file upload limits. It
 * uses a
 * builder pattern for construction and provides a factory method to create
 * instances
 * from a {@link Properties} object with default values if properties are not
 * specified.
 */
public class ServerConfig {

    // Default constants

    /**
     * Default port number for the server (8080).
     */
    private static final int DEFAULT_PORT = 8080;

    /**
     * Default temporary directory for server files ("tmp/tomcat").
     */
    private static final String DEFAULT_TEMP_DIR = "tmp/tomcat";

    /**
     * Default directory listing enabled state (false).
     */
    private static final boolean DEFAULT_DIRECTORY_LISTING = false;

    /**
     * Default directory listing path
     */
    private static final String DEFAULT_DIRECTORY_LISTING_PATH = "/directory";

    /**
     * Default SSL enabled state (false).
     */
    private static final boolean DEFAULT_SSL_ENABLED = false;

    /**
     * Default SSL port number (8443).
     */
    private static final int DEFAULT_SSL_PORT = 8443;

    /**
     * Default keystore path (empty string).
     */
    private static final String DEFAULT_KEYSTORE = "";

    /**
     * Default keystore password (empty string).
     */
    private static final String DEFAULT_KEYSTORE_PASSWORD = "";

    /**
     * Default key alias (empty string).
     */
    private static final String DEFAULT_KEY_ALIAS = "";

    /**
     * Default minimum number of threads in the thread pool (10).
     */
    private static final int DEFAULT_THREADS_MIN = 10;

    /**
     * Default maximum number of threads in the thread pool (200).
     */
    private static final int DEFAULT_THREADS_MAX = 200;

    /**
     * Default thread timeout in milliseconds (60000, i.e., 60 seconds).
     */
    private static final long DEFAULT_THREADS_TIMEOUT = 60000;

    /**
     * Default daemon thread setting (false).
     */
    private static final boolean DEFAULT_DAEMON = false;

    /**
     * Default application name ("JoltApp").
     */
    private static final String DEFAULT_APP_NAME = "JoltApp";

    /**
     * Default maximum file size for multipart uploads (15MB).
     */
    private static final long DEFAULT_MULTIPART_MAX_FILE_SIZE = 15 * 1024L * 1024L; // 15MB

    /**
     * Default maximum request size for multipart uploads (50MB).
     */
    private static final long DEFAULT_MULTIPART_MAX_REQUEST_SIZE = 50 * 1024L * 1024L; // 50MB

    /**
     * Default file size threshold for multipart uploads before writing to disk
     * (1MB).
     */
    private static final int DEFAULT_MULTIPART_FILE_SIZE_THRESHOLD = 1024 * 1024; // 1MB

    /**
     * Default HTTP access for port 80
     */
    private static final boolean DEFAULT_HTTP_ACCESS = true;

    // Actual fields

    /**
     * The port number on which the server listens.
     */
    private final int port;

    /**
     * The temporary directory used by the server.
     */
    private final String tempDir;

    /**
     * The directory listing enabled flag.
     */
    private final boolean directoryListing;

    /**
     * The directory listing path.
     */
    private final String directoryListingPath;

    /**
     * Indicates whether SSL is enabled for the server.
     */
    private final boolean sslEnabled;

    /**
     * The port number for SSL connections.
     */
    private final int sslPort;

    /**
     * The path to the SSL keystore file.
     */
    private final String keyStore;

    /**
     * The password for the SSL keystore.
     */
    private final String keyStorePassword;

    /**
     * The alias of the key in the SSL keystore.
     */
    private final String keyAlias;

    /**
     * The minimum number of threads in the server's thread pool.
     */
    private final int threadsMin;

    /**
     * The maximum number of threads in the server's thread pool.
     */
    private final int threadsMax;

    /**
     * The timeout in milliseconds for threads in the thread pool.
     */
    private final long threadsTimeout;

    /**
     * Indicates whether the server runs as a daemon process.
     */
    private final boolean daemon;

    /**
     * The name of the application.
     */
    private final String appName;

    /**
     * The maximum file size allowed for multipart uploads, in bytes.
     */
    private final long multipartMaxFileSize;

    /**
     * The maximum request size allowed for multipart uploads, in bytes.
     */
    private final long multipartMaxRequestSize;

    /**
     * The file size threshold for multipart uploads before writing to disk, in
     * bytes.
     */
    private final int multipartFileSizeThreshold;

    /**
     * Indicates whether http-access is allowed from 80 port. Default is true.
     */
    private final boolean httpEnabled;

    /**
     * Constructs a new ServerConfig instance using the provided builder.
     *
     * @param builder The {@link Builder} containing the configuration settings.
     */
    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.tempDir = builder.tempDir;
        this.directoryListing = builder.directoryListing;
        this.directoryListingPath = builder.directoryListingPath;
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
        this.httpEnabled = builder.httpEnabled;
    }

    // Getters...

    /**
     * Returns the port number on which the server listens.
     *
     * @return The server port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the temporary directory used by the server.
     *
     * @return The path to the temporary directory.
     */
    public String getTempDir() {
        return tempDir;
    }

    /**
     * Indicates whether directory listing is enabled.
     * 
     * @return {@code True} if directory listing is enabled, {@code False}
     *         otherwise.
     */
    public boolean isDirectoryListingEnabled() {
        return directoryListing;
    }

    /**
     * Returns the path to the directory listing file.
     * 
     * @return The path to the directory listing.
     */
    public String getDirectoryListingPath() {
        return directoryListingPath;
    }

    /**
     * Indicates whether SSL is enabled for the server.
     *
     * @return {@code true} if SSL is enabled, {@code false} otherwise.
     */
    public boolean isSslEnabled() {
        return sslEnabled;
    }

    /**
     * Returns the port number for SSL connections.
     *
     * @return The SSL port number.
     */
    public int getSslPort() {
        return sslPort;
    }

    /**
     * Returns the path to the SSL keystore file.
     *
     * @return The keystore file path.
     */
    public String getKeyStore() {
        return keyStore;
    }

    /**
     * Returns the password for the SSL keystore.
     *
     * @return The keystore password.
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * Returns the alias of the key in the SSL keystore.
     *
     * @return The key alias.
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Returns the minimum number of threads in the server's thread pool.
     *
     * @return The minimum thread count.
     */
    public int getThreadsMin() {
        return threadsMin;
    }

    /**
     * Returns the maximum number of threads in the server's thread pool.
     *
     * @return The maximum thread count.
     */
    public int getThreadsMax() {
        return threadsMax;
    }

    /**
     * Returns the timeout in milliseconds for threads in the thread pool.
     *
     * @return The thread timeout in milliseconds.
     */
    public long getThreadsTimeout() {
        return threadsTimeout;
    }

    /**
     * Indicates whether the server runs as a daemon process.
     *
     * @return {@code true} if the server is a daemon, {@code false} otherwise.
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * Returns the name of the application.
     *
     * @return The application name.
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns the maximum file size allowed for multipart uploads.
     *
     * @return The maximum file size in bytes.
     */
    public long getMultipartMaxFileSize() {
        return multipartMaxFileSize;
    }

    /**
     * Returns the maximum request size allowed for multipart uploads.
     *
     * @return The maximum request size in bytes.
     */
    public long getMultipartMaxRequestSize() {
        return multipartMaxRequestSize;
    }

    /**
     * Returns the file size threshold for multipart uploads before writing to disk.
     *
     * @return The file size threshold in bytes.
     */
    public int getMultipartFileSizeThreshold() {
        return multipartFileSizeThreshold;
    }

    /**
     * Indicates whether the server allow connection from HTTP.
     * 
     * @return {@code true} if the server allow connection from HTTP, {@code false}
     *         otherwise.
     */
    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    /**
     * Builder class for constructing {@link ServerConfig} instances.
     * <p>
     * Provides a fluent API to set server configuration properties, using default
     * values if not explicitly specified.
     */
    public static class Builder {
        private int port = DEFAULT_PORT;
        private String tempDir = DEFAULT_TEMP_DIR;
        private boolean directoryListing = DEFAULT_DIRECTORY_LISTING;
        private String directoryListingPath = DEFAULT_DIRECTORY_LISTING_PATH;
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
        private boolean httpEnabled = DEFAULT_HTTP_ACCESS;

        /**
         * Sets the port number for the server.
         *
         * @param port The port number to listen on.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the temporary directory for the server.
         *
         * @param tempDir The path to the temporary directory.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder tempDir(String tempDir) {
            this.tempDir = tempDir;
            return this;
        }

        public Builder directoryListing(boolean directoryListing) {
            this.directoryListing = directoryListing;
            return this;
        }

        public Builder directoryListingPath(String directoryListingPath) {
            this.directoryListingPath = directoryListingPath;
            return this;
        }

        /**
         * Enables or disables SSL for the server.
         *
         * @param sslEnabled {@code true} to enable SSL, {@code false} otherwise.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder sslEnabled(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
            return this;
        }

        /**
         * Sets the SSL port number for the server.
         *
         * @param sslPort The SSL port number.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder sslPort(int sslPort) {
            this.sslPort = sslPort;
            return this;
        }

        /**
         * Sets the path to the SSL keystore file.
         *
         * @param keyStore The keystore file path.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder keyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        /**
         * Sets the password for the SSL keystore.
         *
         * @param keyStorePassword The keystore password.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder keyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
            return this;
        }

        /**
         * Sets the alias of the key in the SSL keystore.
         *
         * @param keyAlias The key alias.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder keyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
            return this;
        }

        /**
         * Sets the minimum number of threads in the server's thread pool.
         *
         * @param threadsMin The minimum thread count.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder threadsMin(int threadsMin) {
            this.threadsMin = threadsMin;
            return this;
        }

        /**
         * Sets the maximum number of threads in the server's thread pool.
         *
         * @param threadsMax The maximum thread count.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder threadsMax(int threadsMax) {
            this.threadsMax = threadsMax;
            return this;
        }

        /**
         * Sets the timeout in milliseconds for threads in the thread pool.
         *
         * @param threadsTimeout The thread timeout in milliseconds.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder threadsTimeout(long threadsTimeout) {
            this.threadsTimeout = threadsTimeout;
            return this;
        }

        /**
         * Sets whether the server runs as a daemon process.
         *
         * @param daemon {@code true} to run as a daemon, {@code false} otherwise.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        /**
         * Sets the name of the application.
         *
         * @param appName The application name.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        /**
         * Sets the maximum file size for multipart uploads.
         *
         * @param size The maximum file size in bytes.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder multipartMaxFileSize(long size) {
            this.multipartMaxFileSize = size;
            return this;
        }

        /**
         * Sets the maximum request size for multipart uploads.
         *
         * @param size The maximum request size in bytes.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder multipartMaxRequestSize(long size) {
            this.multipartMaxRequestSize = size;
            return this;
        }

        /**
         * Sets the file size threshold for multipart uploads before writing to disk.
         *
         * @param threshold The file size threshold in bytes.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder multipartFileSizeThreshold(int threshold) {
            this.multipartFileSizeThreshold = threshold;
            return this;
        }

        /**
         * Sets whether the server accept HTTP port 80 connection or not.
         * 
         * @param httpEnabled Whether the server accept HTTP port 80 connection or not.
         * @return This {@code Builder} instance for chaining.
         */
        public Builder httpEnabled(boolean httpEnabled) {
            this.httpEnabled = httpEnabled;
            return this;
        }

        /**
         * Builds a new {@link ServerConfig} instance with the configured settings.
         *
         * @return A new {@code ServerConfig} instance.
         */
        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }

    /**
     * Creates a ServerConfig instance from a properties object.
     * <p>
     * Retrieves server settings from the provided {@link Properties} object using
     * specific keys (e.g., {@code server.port}, {@code server.ssl.enabled}). If a
     * property is not found or cannot be parsed, the corresponding default value
     * is used (e.g., {@value #DEFAULT_PORT}, {@value #DEFAULT_SSL_ENABLED}).
     *
     * @param props The {@link Properties} object containing server configuration
     *              settings.
     * @return A new {@code ServerConfig} instance with the loaded settings.
     */
    public static ServerConfig fromProperties(Properties props) {
        Builder builder = new Builder();
        builder.port(Integer.parseInt(props.getProperty("server.port", String.valueOf(DEFAULT_PORT))));
        builder.tempDir(props.getProperty("server.tempDir", DEFAULT_TEMP_DIR));
        builder.directoryListing(
                Boolean.parseBoolean(
                        props.getProperty("server.directory.listing", String.valueOf(DEFAULT_DIRECTORY_LISTING))));
        builder.directoryListingPath(
                props.getProperty("server.directory.listing.path", DEFAULT_DIRECTORY_LISTING_PATH));
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
        builder.httpEnabled(Boolean.parseBoolean(props.getProperty("server.http.enabled", String.valueOf(true))));
        return builder.build();
    }
}