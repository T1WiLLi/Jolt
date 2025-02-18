package ca.jolt.tomcat.config;

import ca.jolt.tomcat.abstraction.ConfigurationBuilder;

public class ThreadConfigBuilder extends ConfigurationBuilder<ThreadConfig> {
    private final ServerConfigBuilder parentBuilder;
    private final ThreadConfig config;

    public ThreadConfigBuilder(ServerConfigBuilder parentBuilder, ServerConfig serverConfig) {
        this.parentBuilder = parentBuilder;
        this.config = serverConfig.getThreads();
    }

    public ThreadConfigBuilder withMinThreads(int minThreads) {
        config.setMinThreads(minThreads);
        return this;
    }

    public ThreadConfigBuilder withMaxThreads(int maxThreads) {
        config.setMaxThreads(maxThreads);
        return this;
    }

    public ThreadConfigBuilder withTimeout(long timeout) {
        config.setTimeout(timeout);
        return this;
    }

    public ServerConfigBuilder and() {
        return parentBuilder;
    }

    @Override
    public ThreadConfig build() {
        validate();
        return config;
    }

    @Override
    protected void validate() {
        if (config.getMinThreads() <= 0 || config.getMaxThreads() <= 0 || config.getTimeout() <= 0) {
            throw new IllegalStateException("Thread configuration values must be positive.");
        }
    }
}