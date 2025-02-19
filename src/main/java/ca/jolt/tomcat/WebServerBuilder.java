package ca.jolt.tomcat;

import ca.jolt.tomcat.abstraction.WebServer;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.config.ServerConfigBuilder;
import ca.jolt.tomcat.config.SslConfigBuilder;
import ca.jolt.tomcat.config.ThreadConfigBuilder;

public class WebServerBuilder {
    private final ServerConfigBuilder configBuilder;

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

    public ThreadConfigBuilder withThreads() {
        return new ThreadConfigBuilder(this, configBuilder.getConfig());
    }

    public SslConfigBuilder withSsl() {
        return new SslConfigBuilder(this, configBuilder.getConfig());
    }

    public WebServer build() {
        ServerConfig config = configBuilder.build();
        return new TomcatServer(config);
    }
}