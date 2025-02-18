package ca.jolt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_TEMP_DIR = "tmp/tomcat";

    private Integer port;
    private String tempDir;
    private ThreadConfig threads;
    private SslConfig ssl;

    public ServerConfig() {
        // Initialize with default values
        this.port = DEFAULT_PORT;
        this.tempDir = DEFAULT_TEMP_DIR;
        this.threads = new ThreadConfig();
        this.ssl = new SslConfig();
    }

    // Getters with default values
    public int getPort() {
        return port != null ? port : DEFAULT_PORT;
    }

    public String getTempDir() {
        return tempDir != null ? tempDir : DEFAULT_TEMP_DIR;
    }

    public ThreadConfig getThreads() {
        return threads != null ? threads : new ThreadConfig();
    }

    public SslConfig getSsl() {
        return ssl != null ? ssl : new SslConfig();
    }

    // Setters
    public void setPort(Integer port) {
        this.port = port;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public void setThreads(ThreadConfig threads) {
        this.threads = threads;
    }

    public void setSsl(SslConfig ssl) {
        this.ssl = ssl;
    }
}