package ca.jolt.server.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
public class ThreadConfig {
    private static final int DEFAULT_MIN_THREADS = 10;
    private static final int DEFAULT_MAX_THREADS = 200;
    private static final long DEFAULT_TIMEOUT = 60000;

    private Integer minThreads;
    private Integer maxThreads;
    private Long timeout;

    public ThreadConfig() {
        this.minThreads = DEFAULT_MIN_THREADS;
        this.maxThreads = DEFAULT_MAX_THREADS;
        this.timeout = DEFAULT_TIMEOUT;
    }

    public int getMinThreads() {
        return minThreads != null ? minThreads : DEFAULT_MIN_THREADS;
    }

    public int getMaxThreads() {
        return maxThreads != null ? maxThreads : DEFAULT_MAX_THREADS;
    }

    public long getTimeout() {
        return timeout != null ? timeout : DEFAULT_TIMEOUT;
    }
}