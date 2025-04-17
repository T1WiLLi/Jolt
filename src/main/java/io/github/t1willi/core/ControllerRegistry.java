package io.github.t1willi.core;

import java.util.List;
import java.util.logging.Logger;

import io.github.t1willi.injector.JoltContainer;

public final class ControllerRegistry {
    private static final Logger logger = Logger.getLogger(ControllerRegistry.class.getName());

    private final Router router;

    public ControllerRegistry() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
    }

    public void registerControllers() {
        List<BaseController> controllers = JoltContainer.getInstance().getBeans(BaseController.class);
    }
}