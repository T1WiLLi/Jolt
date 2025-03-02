package ca.jolt.routing;

import java.util.function.Supplier;

import ca.jolt.exceptions.JoltRoutingException;
import ca.jolt.routing.context.JoltHttpContext;

/**
 * A functional interface representing a handler for a matched route.
 * <p>
 * The {@code handle} method accepts a {@link JoltHttpContext} and
 * returns an object, which might be used as the response body in Jolt.
 * </p>
 *
 * <p>
 * If a route should return a simple static response, you can convert
 * a {@link Supplier} to a {@code RouteHandler} using
 * {@link #fromSupplier(Supplier)}.
 * </p>
 *
 * @see JoltHttpContext
 * @see Route
 * @since 1.0
 */
@FunctionalInterface
public interface RouteHandler {

    /**
     * The main method invoked when a request matches a route.
     * <p>
     * This method receives a {@link JoltHttpContext} allowing
     * access to request data (e.g., path/query parameters, body, etc.)
     * and response methods (e.g., setting headers or cookies).
     * </p>
     *
     * @param ctx
     *            The request/response context object for this route.
     * @return
     *         An object, which may be used as the response body
     *         by the Jolt framework.
     * @throws JoltRoutingException
     *                              If an error occurs during the routing or
     *                              handling process.
     */
    Object handle(JoltHttpContext ctx) throws JoltRoutingException;

    /**
     * Converts a {@link Supplier} returning an {@code Object} into a
     * {@code RouteHandler}. This is useful for routes that produce a simple
     * static or memoized response and do not need direct access to the
     * {@link JoltHttpContext}.
     *
     * @param supplier
     *                 A supplier returning the object to be used as the response
     *                 body.
     * @return
     *         A {@code RouteHandler} that ignores the context and uses
     *         the supplied object's value.
     */
    static RouteHandler fromSupplier(Supplier<Object> supplier) {
        return ctx -> supplier.get();
    }
}