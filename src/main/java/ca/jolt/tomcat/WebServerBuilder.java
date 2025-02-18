package ca.jolt.tomcat;

import ca.jolt.tomcat.abstraction.WebServer;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.config.ServerConfigBuilder;
import ca.jolt.tomcat.config.SslConfigBuilder;
import ca.jolt.tomcat.config.ThreadConfigBuilder;

public class WebServerBuilder {
    private final ServerConfigBuilder configBuilder;

    public WebServerBuilder() {
        this.configBuilder = new ServerConfigBuilder();
    }

    public final WebServerBuilder withPort(int port) {
        configBuilder.withPort(port);
        return this;
    }

    public final WebServerBuilder withTempDir(String tempDir) {
        configBuilder.withTempDir(tempDir);
        return this;
    }

    public final SslConfigBuilder withSsl() {
        return configBuilder.withSsl();
    }

    public final ThreadConfigBuilder withThreads() {
        return configBuilder.withThreads();
    }

    /**
     * Builds the WebServer using the configured ServerConfig.
     * You can override this method to return a different implementation of
     * WebServer.
     *
     * @return {@link WebServer}
     * @implNote This method is called after all configuration methods have been.
     */
    public WebServer build() {
        ServerConfig config = configBuilder.build();
        return new TomcatServer(config);
    }
}