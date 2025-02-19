package ca.jolt.tomcat.config;

import ca.jolt.tomcat.WebServerBuilder;
import ca.jolt.tomcat.abstraction.ConfigurationBuilder;

public class SslConfigBuilder implements ConfigurationBuilder<SslConfig> {
    private final WebServerBuilder parentBuilder;
    private final SslConfig config;

    public SslConfigBuilder(WebServerBuilder parentBuilder, ServerConfig serverConfig) {
        this.parentBuilder = parentBuilder;
        this.config = serverConfig.getSsl();
    }

    public SslConfigBuilder withEnabled(boolean enabled) {
        config.setEnabled(enabled);
        return this;
    }

    public SslConfigBuilder withPort(int port) {
        config.setPort(port);
        return this;
    }

    public SslConfigBuilder withKeystore(String keystorePath, String keystorePassword) {
        config.setKeystorePath(keystorePath);
        config.setKeystorePassword(keystorePassword);
        return this;
    }

    public SslConfigBuilder withKeyAlias(String keyAlias) {
        config.setKeyAlias(keyAlias);
        return this;
    }

    public WebServerBuilder and() {
        return parentBuilder;
    }

    @Override
    public SslConfig build() {
        validate();
        return config;
    }

    @Override
    public void validate() {
        if (config.isEnabled()) {
            if (config.getKeystorePath() == null || config.getKeystorePassword() == null) {
                throw new IllegalStateException("Keystore path and password are required when SSL is enabled.");
            }
            if (config.getKeyAlias() == null) {
                throw new IllegalStateException("Key alias is required when SSL is enabled.");
            }
            if (config.getPort() <= 0) {
                throw new IllegalStateException("SSL port must be positive when SSL is enabled.");
            }
        }
    }
}