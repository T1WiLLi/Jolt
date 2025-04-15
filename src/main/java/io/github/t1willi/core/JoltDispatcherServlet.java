package io.github.t1willi.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.exceptions.JoltRoutingException;
import io.github.t1willi.exceptions.handler.ExceptionHandler;
import io.github.t1willi.exceptions.handler.GlobalExceptionHandler;
import io.github.t1willi.filters.FilterConfiguration;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.LifecycleEntry;
import io.github.t1willi.routing.MimeInterpreter;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.routing.RouteMatch;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.utils.DirectoryListingHtmlTemplateBuilder;
import io.github.t1willi.utils.HelpMethods;

/**
 * The main servlet responsible for dispatching HTTP requests to the appropriate
 * route handler or serving static resources in a Jolt-based application.
 * <p>
 * This servlet handles the following steps for each request:
 * <ol>
 * <li>Processes registered filters.</li>
 * <li>Executes "before" handlers.</li>
 * <li>Attempts to match the request to a route or serve a static resource.</li>
 * <li>Returns a 404 error if no route or static resource is found.</li>
 * <li>Executes "after" handlers.</li>
 * </ol>
 * <p>
 * Directory listings are explicitly disabled, and static resources are only
 * served
 * if they exist in the "static" directory of the classpath.
 *
 * @author William
 * @since 1.0
 */
public final class JoltDispatcherServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(JoltDispatcherServlet.class.getName());
    private static final ThreadLocal<JoltContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private transient final Router router;
    private transient final ExceptionHandler exceptionHandler;

    public static JoltContext getCurrentContext() {
        return CURRENT_CONTEXT.get();
    }

    /**
     * Constructs a new {@code JoltDispatcherServlet}, retrieving necessary
     * components from the {@link JoltContainer} IoC container.
     */
    public JoltDispatcherServlet() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
        this.exceptionHandler = JoltContainer.getInstance().getBean(GlobalExceptionHandler.class);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        RequestContext context = prepareRequestContext(req, res);
        JoltContext joltCtx = new JoltContext(req, res, null, Collections.emptyList());
        try {
            CURRENT_CONTEXT.set(joltCtx);
            if (processFilters(context)) {
                executeAfterHandlers(joltCtx);
                return;
            }

            executeBeforeHandlers(joltCtx);

            if (!handleRequest(context, joltCtx, start)) {
                sendNotFoundError(context);
            }
        } catch (Exception e) {
            log.warning(() -> "Error in request processing: " + e.getMessage());
            if (!context.res.isCommitted() && joltCtx != null) {
                joltCtx.commit();
                log.info("Committed context with status: " + joltCtx.getStatus().code());
            }
            exceptionHandler.handleException(e, joltCtx.getResponse());
        } finally {
            if (!context.res.isCommitted() && joltCtx != null) {
                joltCtx.commit();
                executeAfterHandlers(joltCtx);
            }
            CURRENT_CONTEXT.remove();
        }
    }

    /**
     * Prepares the request context by extracting the method and path from the
     * request.
     *
     * @param req The HTTP request
     * @param res The HTTP response
     * @return A new {@link RequestContext} containing request details
     */
    private RequestContext prepareRequestContext(HttpServletRequest req, HttpServletResponse res) {
        String method = req.getMethod();
        String path = getPath(req);
        log.info(() -> String.format("Incoming %s %s from %s", method, path, req.getRemoteAddr()));
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
                    return Integer.compare(order1, order2);
                }).toList();

        JoltContext joltContext = new JoltContext(context.req, context.res, null, Collections.emptyList());

        for (JoltFilter filter : filters) {
            if (filterConfig.shouldExcludeRoute(joltContext)) {
                continue;
            }
            try {
                filter.doFilter(context.req, context.res, (request, response) -> {
                    // No-op. The filter itself controls whether to pass to the next filter.
                });
            } catch (IOException | ServletException e) {
                exceptionHandler.handleException(e, context.res);
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
     * @param ctx The Jolt context containing request and response
     */
    private void executeBeforeHandlers(JoltContext ctx) {
        executeHandlers(ctx, router.getBeforeHandlers().stream());
    }

    /**
     * Executes all registered "after" handlers that match the current path.
     *
     * @param ctx The Jolt context containing request and response
     */
    private void executeAfterHandlers(JoltContext ctx) {
        executeHandlers(ctx, router.getAfterHandlers().stream());
    }

    /**
     * Executes a stream of handlers that match the current path.
     *
     * @param ctx      The Jolt context containing request and response
     * @param handlers The stream of handlers to execute
     */
    private void executeHandlers(JoltContext ctx, Stream<LifecycleEntry> handlers) {
        handlers.filter(entry -> entry.matches(ctx.requestPath()))
                .forEach(entry -> {
                    try {
                        entry.execute(ctx);
                    } catch (Exception e) {
                        exceptionHandler.handleException(e, ctx.getResponse());
                    }
                });
    }

    /**
     * Handles the request by attempting to match a route or serve a static
     * resource.
     *
     * @param context The request context
     * @param joltCtx The Jolt context
     * @param start   The start time of the request
     * @return {@code true} if the request was handled, {@code false} otherwise
     */
    private boolean handleRequest(RequestContext context, JoltContext joltCtx, long start) {
        RouteMatch match = router.match(context.method, context.path);
        if (match != null) {
            return handleRoute(match, context, start);
        }

        if (context.method.equals("GET")) {
            if (tryServeStaticResource(context.path, context.res)) {
                return true;
            }
            if (DirectoryListingHtmlTemplateBuilder.tryServeDirectoryListing(context.path, context.res)) {
                return true;
            }
        }

        if (router.pathExistsWithDifferentMethod(context.method, context.path)) {
            log.info(() -> "Method " + context.method + " not allowed for path: " + context.path);
            String allowedMethods = router.getAllowedMethods(context.path);
            context.res.setHeader("Allow", allowedMethods);
            exceptionHandler.handleException(
                    new JoltHttpException(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed for " + context.path),
                    context.res);
            return true;
        }
        return false;
    }

    /**
     * Sends a 404 Not Found response.
     *
     * @param context The request context
     */
    private void sendNotFoundError(RequestContext context) {
        log.info(() -> "No route or static resource found for path: " + context.path);
        exceptionHandler.handleException(
                new JoltHttpException(HttpStatus.NOT_FOUND, "No route or static resource found for " + context.path),
                context.res);
    }

    /**
     * Attempts to serve a static resource from the "static" directory in the
     * classpath.
     * Strips web context path prefixes and serves resources relative to the static
     * root.
     *
     * @param path The requested path
     * @param res  The HTTP response
     * @return {@code true} if the resource was served, {@code false} otherwise
     */
    private boolean tryServeStaticResource(String path, HttpServletResponse res) {
        try {
            if (!HelpMethods.isValidStaticResourcePath(path)) {
                log.warning(() -> "Illegal static resource path: " + path);
                return false;
            }

            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            String resourcePath = "static/" + normalizedPath;

            log.info(() -> "Attempting to serve static file: " + resourcePath);

            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (in != null) {
                byte[] data = in.readAllBytes();
                String extension = resourcePath.substring(resourcePath.lastIndexOf('.'));
                res.setContentType(MimeInterpreter.getMime(extension));
                res.getOutputStream().write(data);
                return true;
            } else {
                log.warning(() -> "Static resource not found: " + resourcePath);
                return false;
            }
        } catch (IOException e) {
            log.warning(() -> "Error serving static resource: " + path + " - " + e.getMessage());
        }
        return false;
    }

    /**
     * Handles a matched route by executing its handler and processing the result.
     *
     * @param match   The route match containing handler and path parameters
     * @param context The request context
     * @param start   The start time of the request
     * @return {@code true} if the route was handled, {@code false} otherwise
     */
    private boolean handleRoute(RouteMatch match, RequestContext context, long start) {
        try {
            JoltContext routeCtx = new JoltContext(context.req, context.res, match.matcher(),
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
            exceptionHandler.handleException(e, context.res);
            return false;
        }
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
     * Holds context information for a single request.
     */
    private static class RequestContext {
        final HttpServletRequest req;
        final HttpServletResponse res;
        final String method;
        final String path;

        RequestContext(HttpServletRequest req, HttpServletResponse res, String method, String path) {
            this.req = req;
            this.res = res;
            this.method = method;
            this.path = path;
        }
    }
}