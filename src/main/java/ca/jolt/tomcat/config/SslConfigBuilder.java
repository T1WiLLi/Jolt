package ca.jolt.tomcat.config;

import ca.jolt.tomcat.abstraction.ConfigurationBuilder;

public class SslConfigBuilder extends ConfigurationBuilder<SslConfig> {
    private final ServerConfigBuilder parentBuilder;
    private final SslConfig config;

    public SslConfigBuilder(ServerConfigBuilder parentBuilder, ServerConfig serverConfig) {
        this.parentBuilder = parentBuilder;
        this.config = serverConfig.getSsl();
    }

    public SslConfigBuilder withEnabled(boolean enabled) {
        config.setEnabled(enabled);
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

    public ServerConfigBuilder and() {
        return parentBuilder;
    }

    @Override
    public SslConfig build() {
        validate();
        return config;
    }

    @Override
    protected void validate() {
        if (config.isEnabled() && (config.getKeystorePath() == null || config.getKeystorePassword() == null)) {
            throw new IllegalStateException("Keystore path and password are required when SSL is enabled.");
        }
    }
}