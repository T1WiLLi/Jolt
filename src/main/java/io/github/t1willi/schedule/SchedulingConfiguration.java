package io.github.t1willi.schedule;

import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.injector.type.BeanScope;
import io.github.t1willi.injector.type.InitializationMode;
import io.github.t1willi.server.config.ConfigurationManager;
import lombok.Getter;

@JoltBean(initialization = InitializationMode.EAGER, scope = BeanScope.SINGLETON)
public final class SchedulingConfiguration {
    private final int DEFAULT_POOL_SIZE = 10;
    @Getter
    private int poolSize;

    public SchedulingConfiguration() {
        this.poolSize = Integer.parseInt(
                ConfigurationManager.getInstance().getProperty("server.schedule.poolsize",
                        String.valueOf(DEFAULT_POOL_SIZE)));
    }
}
