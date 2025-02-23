package ca.jolt.server.config;

import ca.jolt.server.WebServerBuilder;
import ca.jolt.server.abstraction.ConfigurationBuilder;

public class ThreadConfigBuilder implements ConfigurationBuilder<ThreadConfig> {
    private final WebServerBuilder parentBuilder;
    private final ThreadConfig config;

    public ThreadConfigBuilder(WebServerBuilder parentBuilder, ServerConfig serverConfig) {
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

    public WebServerBuilder and() {
        return parentBuilder;
    }

    @Override
    public ThreadConfig build() {
        validate();
        return config;
    }

    @Override
    public void validate() {
        if (config.getMinThreads() <= 0 || config.getMaxThreads() <= 0 || config.getTimeout() <= 0) {
            throw new IllegalStateException("Thread configuration values must be positive.");
        }
        if (config.getMinThreads() > config.getMaxThreads()) {
            throw new IllegalStateException("Minimum threads cannot be greater than maximum threads.");
        }
    }
}