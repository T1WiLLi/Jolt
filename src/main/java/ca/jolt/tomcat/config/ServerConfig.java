package ca.jolt.tomcat.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
public class ServerConfig {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_TEMP_DIR = "tmp/tomcat";

    private Integer port;
    private String tempDir;
    private ThreadConfig threads;
    private SslConfig ssl;

    public ServerConfig() {
        this.port = DEFAULT_PORT;
        this.tempDir = DEFAULT_TEMP_DIR;
        this.threads = new ThreadConfig();
        this.ssl = new SslConfig();
    }

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
}