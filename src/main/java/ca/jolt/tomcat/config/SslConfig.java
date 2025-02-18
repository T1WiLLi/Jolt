package ca.jolt.tomcat.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
public class SslConfig {
    private static final int DEFAULT_SSL_PORT = 8443;

    private Boolean enabled;
    private Integer port;
    private String keystorePath;
    private String keystorePassword;
    private String keyAlias;

    public SslConfig() {
        this.enabled = false;
        this.port = DEFAULT_SSL_PORT;
    }

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
}