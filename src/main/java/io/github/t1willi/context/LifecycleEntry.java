package io.github.t1willi.context;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an entry in the HTTP request/response lifecycle.
 * This class manages the execution of handlers for specific paths in the
 * request lifecycle.
 */
public final class LifecycleEntry {
    private final List<String> paths;
    private final Consumer<JoltContext> handler;

    /**
     * Creates a new lifecycle entry with the specified paths and handler.
     *
     * @param paths   The list of URL paths that this lifecycle entry should handle.
     *                If null, defaults to an empty list, which matches all routes.
     * @param handler The handler to execute when a matching path is encountered.
     *                Cannot be null.
     * @throws NullPointerException if the handler is null
     */
    public LifecycleEntry(List<String> paths, Consumer<JoltContext> handler) {
        this.paths = (paths == null) ? List.of() : paths;
        this.handler = Objects.requireNonNull(handler, "Handler cannot be null");
    }

    /**
     * Determines whether this lifecycle entry should handle the given request path.
     *
     * @param requestPath The path of the current request to check against
     * @return true if this entry should handle the given path; false otherwise.
     *         Returns true for all paths if the paths list is empty.
     */
    public boolean matches(String requestPath) {
        if (paths.isEmpty()) {
            return true; // All routes.
        }
        return paths.contains(requestPath);
    }

    /**
     * Executes the handler for this lifecycle entry with the given HTTP context.
     *
     * @param ctx The HTTP context for the current request
     */
    public void execute(JoltContext ctx) {
        handler.accept(ctx);
    }
}