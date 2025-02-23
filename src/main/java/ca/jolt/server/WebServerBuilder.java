package ca.jolt.server;

import ca.jolt.exceptions.ServerException;
import ca.jolt.server.abstraction.WebServer;
import ca.jolt.server.config.ServerConfig;
import ca.jolt.server.config.ServerConfigBuilder;
import ca.jolt.server.config.SslConfigBuilder;
import ca.jolt.server.config.ThreadConfigBuilder;

public class WebServerBuilder {
    private final ServerConfigBuilder configBuilder;
    private WebServerType webServerType = WebServerType.TOMCAT; // Default to Tomcat

    public WebServerBuilder() {
        this.configBuilder = new ServerConfigBuilder(this);
    }

    public WebServerBuilder withPort(int port) {
        configBuilder.withPort(port);
        return this;
    }

    public WebServerBuilder withTempDir(String tempDir) {
        configBuilder.withTempDir(tempDir);
        return this;
    }

    public WebServerBuilder withServerType(WebServerType type) {
        this.webServerType = type;
        return this;
    }

    public ThreadConfigBuilder withThreads() {
        return new ThreadConfigBuilder(this, configBuilder.getConfig());
    }

    public SslConfigBuilder withSsl() {
        return new SslConfigBuilder(this, configBuilder.getConfig());
    }

    public WebServer build() throws ServerException {
        ServerConfig config = configBuilder.build();
        return WebServerFactory.createServer(this.webServerType, config).finalizeBuild();
    }
}
