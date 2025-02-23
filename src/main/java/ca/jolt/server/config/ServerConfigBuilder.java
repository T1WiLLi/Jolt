package ca.jolt.server.config;

import ca.jolt.server.WebServerBuilder;
import ca.jolt.server.abstraction.ConfigurationBuilder;

public class ServerConfigBuilder implements ConfigurationBuilder<ServerConfig> {
    private final ServerConfig config;
    private final WebServerBuilder parentBuilder;

    public ServerConfigBuilder(WebServerBuilder parentBuilder) {
        this.parentBuilder = parentBuilder;
        this.config = new ServerConfig();
    }

    public ServerConfig getConfig() {
        return config;
    }

    public ServerConfigBuilder withPort(int port) {
        config.setPort(port);
        return this;
    }

    public ServerConfigBuilder withTempDir(String tempDir) {
        config.setTempDir(tempDir);
        return this;
    }

    public SslConfigBuilder withSsl() {
        return new SslConfigBuilder(parentBuilder, config);
    }

    public ThreadConfigBuilder withThreads() {
        return new ThreadConfigBuilder(parentBuilder, config);
    }

    @Override
    public ServerConfig build() {
        validate();
        return config;
    }

    @Override
    public void validate() {
        if (config.getPort() == 0) {
            throw new IllegalStateException("Port is a required configuration.");
        }
        if (config.getTempDir() == null) {
            throw new IllegalStateException("Temp directory is a required configuration.");
        }
    }
}