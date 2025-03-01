package ca.jolt.routing;

import java.util.function.Supplier;

import ca.jolt.exceptions.JoltRoutingException;
import ca.jolt.routing.context.JoltHttpContext;

@FunctionalInterface
public interface RouteHandler {
    Object handle(JoltHttpContext ctx) throws JoltRoutingException;

    static RouteHandler fromSupplier(Supplier<Object> supplier) {
        return ctx -> supplier.get();
    }
}
