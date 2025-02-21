package ca.jolt.routing;

import java.util.function.Supplier;

@FunctionalInterface
public interface RouteHandler {
    Object handle(JoltHttpContext ctx) throws Exception;

    static RouteHandler fromSupplier(Supplier<Object> supplier) {
        return (ctx) -> supplier.get();
    }
}
