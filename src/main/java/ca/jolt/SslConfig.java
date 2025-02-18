package ca.jolt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SslConfig {
    private static final int DEFAULT_SSL_PORT = 8443;

    private Boolean enabled;
    private Integer port;
    private String keystorePath;
    private String keystorePassword;
    private String keyAlias;

    public SslConfig() {
        // Initialize with default values
        this.enabled = false;
        this.port = DEFAULT_SSL_PORT;
    }

    // Getters with default values
    public boolean isEnabled() {
        return enabled != null ? enabled : false;
    }

    public int getPort() {
        return port != null ? port : DEFAULT_SSL_PORT;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    // Setters
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }
}