package io.github.t1willi.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.t1willi.routing.context.JoltContext;

public abstract class BaseController {
    private final List<Consumer<JoltContext>> beforeHandlers = new ArrayList<>();
    private final List<Consumer<JoltContext>> afterHandlers = new ArrayList<>();

    /**
     * Registers a "before" handler that will be executed before any route handler
     * in this controller. The handler will be scoped to the controller's root path.
     *
     * @param handler A handler that receives a {@link JoltContext} for the current
     *                request.
     */
    protected void before(Consumer<JoltContext> handler) {
        beforeHandlers.add(handler);
    }

    /**
     * Registers an "after" handler that will be executed after any route handler
     * in this controller. The handler will be scoped to the controller's root path.
     *
     * @param handler A handler that receives a {@link JoltContext} for the current
     *                request.
     */
    protected void after(Consumer<JoltContext> handler) {
        afterHandlers.add(handler);
    }

    /**
     * Returns the list of "before" handlers registered for this controller.
     *
     * @return The list of before handlers.
     */
    public List<Consumer<JoltContext>> getBeforeHandlers() {
        return beforeHandlers;
    }

    /**
     * Returns the list of "after" handlers registered for this controller.
     *
     * @return The list of after handlers.
     */
    public List<Consumer<JoltContext>> getAfterHandlers() {
        return afterHandlers;
    }
}
