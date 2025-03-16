package ca.jolt.core;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.exceptions.JoltRoutingException;
import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.filters.FilterConfiguration;
import ca.jolt.filters.JoltFilter;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.JoltContainer;
import ca.jolt.routing.LifecycleEntry;
import ca.jolt.routing.MimeInterpreter;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;
import ca.jolt.routing.context.JoltContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * The main servlet responsible for dispatching HTTP requests to the appropriate
 * route handler in a Jolt-based application.
 * <p>
 * It is registered internally within the Jolt framework and uses:
 * <ul>
 * <li>A {@link Router} to match the incoming request path and method.</li>
 * <li>A {@link GlobalExceptionHandler} to handle exceptions thrown during
 * request handling.</li>
 * </ul>
 * <p>
 * On receiving a request,
 * {@link #service(HttpServletRequest, HttpServletResponse)}
 * performs the following steps:
 * <ol>
 * <li>Attempts to serve static resources first.</li>
 * <li>Matches the request to a route.</li>
 * <li>Invokes the corresponding {@link RouteHandler}.</li>
 * <li>Serializes the response as text or JSON, unless already committed.</li>
 * <li>Catches exceptions and delegates handling to the
 * {@link GlobalExceptionHandler}.</li>
 * </ol>
 *
 * @author William
 * @since 1.0
 */
public final class JoltDispatcherServlet extends HttpServlet {

    /**
     * The logger for this servlet.
     */
    private static final Logger log = Logger.getLogger(JoltDispatcherServlet.class.getName());

    /**
     * Matches incoming requests to a {@link RouteHandler}.
     */
    private final transient Router router;

    /**
     * Handles exceptions thrown during request handling.
     */
    private final transient GlobalExceptionHandler exceptionHandler;

    /**
     * Constructs a new {@code JoltDispatcherServlet}, retrieving necessary
     * components from the {@link JoltContainer} IoC container.
     */
    public JoltDispatcherServlet() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
        this.exceptionHandler = JoltContainer.getInstance().getBean(GlobalExceptionHandler.class);
    }

    /**
     * Dispatches incoming HTTP requests to the appropriate route handler.
     * <p>
     * Steps:
     * <ul>
     * <li>Extracts the request path.</li>
     * <li>Tries serving static resources.</li>
     * <li>Finds a matching route and executes its {@link RouteHandler}.</li>
     * <li>Serializes the handler's response if not already committed.</li>
     * <li>Logs request/response details.</li>
     * <li>Delegates exception handling to {@link GlobalExceptionHandler}.</li>
     * </ul>
     *
     * @param req The incoming {@link HttpServletRequest}
     * @param res The outgoing {@link HttpServletResponse}
     * @throws ServletException If a servlet-related error occurs
     * @throws IOException      If an I/O error is detected while handling the
     *                          request
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        RequestContext context = prepareRequestContext(req, res);
        JoltContext joltCtx = new JoltContext(req, res, null, Collections.emptyList());

        try {
            if (processFilters(context)) {
                executeAfterHandlers(joltCtx);
                return;
            }

            executeBeforeHandlers(joltCtx);

            RouteMatch match = router.match(context.method, context.path);
            if (match != null) {
                if (!handleRoute(match, context, start)) {
                    handleStaticResourceOrError(context);
                }
            } else {
                handleStaticResourceOrError(context);
            }
        } catch (Exception e) {
            log.warning(() -> "Error in request processing: " + e.getMessage());
            exceptionHandler.handle(e, context.res);
        } finally {
            if (!context.res.isCommitted() && joltCtx != null) {
                joltCtx.commit();
                executeAfterHandlers(joltCtx);
            }
        }
    }

    /**
     * Prepares an internal request context object with extracted method and path.
     *
     * @param req The HTTP request
     * @param res The HTTP response
     * @return A new {@link RequestContext} containing request details
     */
    private RequestContext prepareRequestContext(HttpServletRequest req, HttpServletResponse res) {
        String method = req.getMethod();
        String path = getPath(req);
        log.info(() -> String.format("Incoming %s %s", method, path));
        return new RequestContext(req, res, method, path);
    }

    /**
     * Processes all registered filters for the current request.
     *
     * @param context The request context
     * @return {@code true} if the request was fully handled by a filter,
     *         {@code false} otherwise
     * @throws ServletException If a filter processing error occurs
     * @throws IOException      If an I/O error occurs during filter processing
     */
    private boolean processFilters(RequestContext context) throws ServletException, IOException {
        FilterConfiguration filterConfig = JoltContainer.getInstance().getBean(FilterConfiguration.class);
        filterConfig.configure();

        List<JoltFilter> filters = JoltContainer.getInstance()
                .getBeans(JoltFilter.class).stream()
                .sorted((f1, f2) -> {
                    int order1 = filterConfig.getOrder(f1);
                    int order2 = filterConfig.getOrder(f2);

                    return order1 != order2 ? Integer.compare(order1, order2) : 0;
                }).toList();

        JoltContext joltContext = new JoltContext(context.req, context.res, null, Collections.emptyList());

        for (JoltFilter filter : filters) {
            if (filterConfig.shouldExcludeRoute(joltContext)) {
                continue;
            }
            try {
                filter.doFilter(context.req, context.res, new FilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException, JoltHttpException {
                        // No-op. The filter itself controls whether to pass to next filter.
                    }
                });
            } catch (IOException | ServletException | JoltHttpException e) {
                exceptionHandler.handle(e, context.res);
            }
            if (context.res.isCommitted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes all registered "before" handlers that match the current path.
     *
     * @param ctx The JoltHttpContext containing request and response
     */
    private void executeBeforeHandlers(JoltContext ctx) {
        executeHandler(ctx, router.getBeforeHandlers().stream());
    }

    /**
     * Executes all registered "after" handlers that match the current path.
     *
     * @param ctx The JoltHttpContext containing request and response
     */
    private void executeAfterHandlers(JoltContext ctx) {
        executeHandler(ctx, router.getAfterHandlers().stream());
    }

    /**
     * Executes a stream of handlers that match the current path.
     *
     * @param ctx      The JoltHttpContext containing request and response
     * @param handlers The stream of handlers to execute
     */
    private void executeHandler(JoltContext ctx, Stream<LifecycleEntry> handlers) {
        try {
            handlers.filter(entry -> entry.matches(ctx.requestPath()))
                    .forEach(entry -> entry.execute(ctx));
        } catch (Exception e) {
            exceptionHandler.handle(e, ctx);
        }
    }

    /**
     * Handles a matched route by executing its handler and processing the result.
     *
     * @param match   The route match containing handler and path parameters
     * @param context The request context
     * @param start   The start time of request processing
     * @return true if the route was successfully handled, false otherwise
     * @throws JoltRoutingException if an I/O error occurs during response writing
     */
    private boolean handleRoute(RouteMatch match, RequestContext context, long start) throws JoltRoutingException {
        try {
            // Update the context with route information
            JoltContext routeCtx = new JoltContext(
                    context.req,
                    context.res,
                    match.matcher(),
                    match.route().getParamNames());

            RouteHandler handler = match.route().getHandler();
            Object result = handler.handle(routeCtx);

            if (!context.res.isCommitted() && result != null && !(result instanceof JoltContext)) {
                if (result instanceof String str) {
                    routeCtx.text(str);
                } else {
                    routeCtx.json(result);
                }
            }

            long duration = System.currentTimeMillis() - start;
            log.info(() -> context.path + " handled successfully in " + duration + "ms");
            if (!context.res.isCommitted() && result instanceof JoltContext joltContext) {
                joltContext.commit();
            }
            return true;
        } catch (JoltRoutingException e) {
            log.warning(() -> "Error in route handler: " + e.getMessage());
            exceptionHandler.handle(e, context.res);
            return false;
        }
    }

    /**
     * Attempts to serve a static resource or handle a not found error.
     *
     * @param context The request context
     * @param start   The start time of request processing
     * @return true if the resource was successfully served, false otherwise
     * @throws JoltHttpException if an I/O error occurs during static resource
     *                           serving
     */
    private boolean handleStaticResourceOrError(RequestContext context) throws JoltHttpException {
        if (tryServeStaticResource(context.path, context.res) && context.method.equals("GET")) {
            return true;
        }

        List<String> allowedMethods = router.getAllowedMethods(context.path);
        if (!allowedMethods.isEmpty()) {
            log.info(() -> "HTTP method " + context.method + " is not allowed for path: " + context.path);
            throw new JoltHttpException(HttpStatus.METHOD_NOT_ALLOWED,
                    "HTTP method " + context.method + " not allowed for " + context.path);
        }

        log.info(() -> "No route found for path: " + context.path);
        exceptionHandler.handle(new JoltHttpException(HttpStatus.NOT_FOUND, "No route found for " + context.path),
                context.res);
        return false;
    }

    /**
     * Determines the request path from {@link HttpServletRequest#getPathInfo()}
     * or falls back to {@link HttpServletRequest#getServletPath()}.
     *
     * @param req The HTTP request
     * @return The normalized path, or {@code "/"} if none is found
     */
    private String getPath(HttpServletRequest req) {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    /**
     * Attempts to serve a static resource from the "static" directory in the
     * classpath.
     * 
     * @param path The requested path, which might correspond to a static resource
     * @param res  The HTTP servlet response
     * @return true if a static resource was found and served, false otherwise
     */
    private boolean tryServeStaticResource(String path, HttpServletResponse res) {
        try {
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            InputStream in = getClass().getClassLoader().getResourceAsStream("static/" + normalizedPath);

            if (in == null && (normalizedPath.isEmpty() || normalizedPath.equals("/"))) {
                normalizedPath = "index.html";
                in = getClass().getClassLoader().getResourceAsStream("static/" + normalizedPath);
            }

            if (in != null) {
                byte[] data = in.readAllBytes();
                int dotIndex = normalizedPath.lastIndexOf('.');
                String extension = (dotIndex != -1) ? normalizedPath.substring(dotIndex) : "";
                String mimeType = MimeInterpreter.getMime(extension);

                res.setContentType(mimeType);
                res.getOutputStream().write(data);
                return true;
            }
        } catch (IOException e) {
            log.warning(() -> "Error serving static resource: " + path + " - " + e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Holds context information for a single request to reduce parameter passing
     * and improve method readability.
     */
    private static class RequestContext {
        final HttpServletRequest req;
        final HttpServletResponse res;
        final String method;
        final String path;

        /**
         * Creates a new {@link RequestContext} for a request/response pair.
         *
         * @param req    The HTTP request
         * @param res    The HTTP response
         * @param method The HTTP method (e.g., "GET", "POST")
         * @param path   The request path
         */
        RequestContext(HttpServletRequest req, HttpServletResponse res, String method, String path) {
            this.req = req;
            this.res = res;
            this.method = method;
            this.path = path;
        }
    }
}