package io.github.t1willi.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import io.github.t1willi.context.JoltContext;

/**
 * Abstract base class for all controllers in the Jolt framework.
 * Controllers must extend this class to be recognized by the framework.
 * Provides methods to register lifecycle handlers that execute before and after
 * route handlers, either for all routes (via before() and after()) or for
 * specific
 * routes (via before(Consumer, String...) and after(Consumer, String...)).
 */
public abstract class BaseController {

    protected final JoltContext context;

    private final List<LifecycleHandler> specificBeforeHandlers = new ArrayList<>();
    private final List<LifecycleHandler> specificAfterHandlers = new ArrayList<>();

    /**
     * General "before" handler that applies to all routes of this controller.
     * Subclasses can override this method to provide custom behavior.
     * The default implementation is a no-op.
     *
     * @param context The current request context.
     */
    public void before(JoltContext context) {
        // No-op by default
    }

    /**
     * General "after" handler that applies to all routes of this controller.
     * Subclasses can override this method to provide custom behavior.
     * The default implementation is a no-op.
     *
     * @param context The current request context.
     */
    public void after(JoltContext context) {
        // No-op by default
    }

    /**
     * Registers a "before" handler for specific routes under this controller.
     * The paths are relative to the controller's root path.
     *
     * @param handler A handler that receives a {@link JoltContext} for the current
     *                request.
     * @param paths   One or more relative paths (e.g., "/{id}", "/create") where
     *                the handler applies.
     */
    protected void before(Consumer<JoltContext> handler, String... paths) {
        specificBeforeHandlers.add(new LifecycleHandler(handler, Arrays.asList(paths)));
    }

    /**
     * Registers an "after" handler for specific routes under this controller.
     * The paths are relative to the controller's root path.
     *
     * @param handler A handler that receives a {@link JoltContext} for the current
     *                request.
     * @param paths   One or more relative paths (e.g., "/{id}", "/create") where
     *                the handler applies.
     */
    protected void after(Consumer<JoltContext> handler, String... paths) {
        specificAfterHandlers.add(new LifecycleHandler(handler, Arrays.asList(paths)));
    }

    /**
     * Returns the list of specific "before" handlers registered for this
     * controller.
     *
     * @return The list of specific before handlers.
     */
    public List<LifecycleHandler> getSpecificBeforeHandlers() {
        return specificBeforeHandlers;
    }

    /**
     * Returns the list of specific "after" handlers registered for this controller.
     *
     * @return The list of specific after handlers.
     */
    public List<LifecycleHandler> getSpecificAfterHandlers() {
        return specificAfterHandlers;
    }

    /**
     * Internal record to store a lifecycle handler and its associated paths.
     */
    protected record LifecycleHandler(Consumer<JoltContext> handler, List<String> paths) {
    }

    protected BaseController() {
        this.context = JoltDispatcherServlet.getCurrentContext();
    }
}