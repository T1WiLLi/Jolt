package ca.jolt.routing;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import ca.jolt.routing.context.JoltHttpContext;

public final class LifecycleEntry {
    private final List<String> paths;
    private final Consumer<JoltHttpContext> handler;

    public LifecycleEntry(List<String> paths, Consumer<JoltHttpContext> handler) {
        this.paths = (paths == null) ? List.of() : paths;
        this.handler = Objects.requireNonNull(handler, "Handler cannot be null");
    }

    public boolean matches(String requestPath) {
        if (paths.isEmpty()) {
            return true; // All routes.
        }
        return paths.contains(requestPath);
    }

    public void execute(JoltHttpContext ctx) {
        handler.accept(ctx);
    }
}
