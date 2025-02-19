package ca.jolt.tomcat;

import ca.jolt.tomcat.abstraction.WebServer;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.config.ServerConfigBuilder;
import ca.jolt.tomcat.config.SslConfigBuilder;
import ca.jolt.tomcat.config.ThreadConfigBuilder;
import ca.jolt.exceptions.ServerException;

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
