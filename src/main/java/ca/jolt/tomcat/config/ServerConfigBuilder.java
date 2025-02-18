package ca.jolt.tomcat.config;

import ca.jolt.tomcat.abstraction.ConfigurationBuilder;

public class ServerConfigBuilder extends ConfigurationBuilder<ServerConfig> {
    private final ServerConfig config;

    public ServerConfigBuilder() {
        this.config = new ServerConfig();
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
        return new SslConfigBuilder(this, config);
    }

    public ThreadConfigBuilder withThreads() {
        return new ThreadConfigBuilder(this, config);
    }

    @Override
    public ServerConfig build() {
        validate();
        return config;
    }

    @Override
    protected void validate() {
        if (config.getPort() == 0) {
            throw new IllegalStateException("Port is a required configuration.");
        }
        if (config.getTempDir() == null) {
            throw new IllegalStateException("Temp directory is a required configuration.");
        }
    }
}