package ca.jolt.core;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.filters.FilterConfiguration;
import ca.jolt.filters.JoltFilter;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.JoltContainer;
import ca.jolt.routing.MimeInterpreter;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;
import ca.jolt.routing.context.JoltHttpContext;
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
import java.util.stream.Collectors;

/**
 * The {@code JoltDispatcherServlet} is the main servlet responsible for
 * dispatching incoming HTTP requests to the appropriate route handler in a
 * Jolt-based
 * application.
 * <p>
 * It is registered internally within the Jolt framework and uses:
 * <ul>
 * <li>A {@link Router} to match the incoming request path and method.</li>
 * <li>A {@link GlobalExceptionHandler} to handle any exceptions thrown during
 * request handling.</li>
 * </ul>
 * <p>
 * Typical usage involves the framework automatically configuring this servlet
 * and mapping it to handle all incoming requests. When a request arrives,
 * {@link #service(HttpServletRequest, HttpServletResponse)} is invoked,
 * which then:
 * <ol>
 * <li>Attempts to serve static resources first.</li>
 * <li>Matches the request to a route.</li>
 * <li>Invokes the corresponding {@link RouteHandler}.</li>
 * <li>Serializes the response, either as text or JSON, unless the handler
 * itself has already committed the response.</li>
 * <li>Catches any thrown exception and delegates handling to the
 * {@link GlobalExceptionHandler}.</li>
 * </ol>
 * </p>
 *
 * @author William Beaudin
 * @since 1.0
 */
public final class JoltDispatcherServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(JoltDispatcherServlet.class.getName());

    /**
     * The {@link Router} used to match incoming requests to {@link RouteHandler}
     * instances.
     */
    private final transient Router router;

    /**
     * A global exception handler that handles all exceptions thrown during
     * request handling.
     */
    private final transient GlobalExceptionHandler exceptionHandler;

    /**
     * Constructs a new {@code JoltDispatcherServlet}, retrieving and storing the
     * necessary components (router and exception handler) from the
     * {@link JoltContainer} IoC container.
     */
    public JoltDispatcherServlet() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
        this.exceptionHandler = JoltContainer.getInstance().getBean(GlobalExceptionHandler.class);
    }

    /**
     * Dispatches incoming HTTP requests to the appropriate route handler.
     * <p>
     * This method:
     * <ul>
     * <li>Extracts the request path using
     * {@link #getPath(HttpServletRequest)}.</li>
     * <li>Tries to serve static resources first.</li>
     * <li>Finds a matching route via the {@link Router}.</li>
     * <li>Executes the matched {@link RouteHandler} if found.</li>
     * <li>Serializes the handler's response as text or JSON, if not already
     * written.</li>
     * <li>Logs the request and response details.</li>
     * <li>Delegates exception handling to the {@link GlobalExceptionHandler}.</li>
     * </ul>
     *
     * @param req
     *            The {@link HttpServletRequest} object that contains the client
     *            request.
     * @param res
     *            The {@link HttpServletResponse} object that contains the servlet's
     *            response.
     * @throws ServletException
     *                          If the request could not be handled due to a
     *                          servlet-related error.
     * @throws IOException
     *                          If an input or output error is detected when the
     *                          servlet handles the request.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        RequestContext context = prepareRequestContext(req, res);

        if (processFilters(context)) {
            return;
        }

        RouteMatch match = router.match(context.method, context.path);
        if (handleRouteMatch(match, context, start)) {
            return;
        }

        handleStaticResourceOrError(context, start);
    }

    /**
     * Prepares a context object to encapsulate request details and reduce
     * parameter passing complexity.
     *
     * @param req The HTTP servlet request
     * @param res The HTTP servlet response
     * @return A context object containing request details
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
     * @param context The request context containing request and response
     * @return true if the request was handled by a filter, false otherwise
     * @throws ServletException if a filter processing error occurs
     * @throws IOException      if an I/O error occurs during filter processing
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
                })
                .collect(Collectors.toList());

        JoltHttpContext joltContext = new JoltHttpContext(context.req, context.res, null, Collections.emptyList());

        for (JoltFilter filter : filters) {
            if (filterConfig.shouldExcludeRoute(joltContext)) {
                continue;
            }
            filter.doFilter(context.req, context.res, new FilterChain() {
                @Override
                public void doFilter(ServletRequest request, ServletResponse response)
                        throws IOException, ServletException {
                    // No-op. The filter itself controls whether to pass to next filter.
                }
            });
            if (context.res.isCommitted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a matched route by executing its handler and processing the result.
     *
     * @param match   The route match containing handler and path parameters
     * @param context The request context
     * @param start   The start time of request processing
     * @return true if the route was successfully handled, false otherwise
     * @throws IOException if an I/O error occurs during response writing
     */
    private boolean handleRouteMatch(RouteMatch match, RequestContext context, long start) throws IOException {
        if (match != null) {
            try {
                JoltHttpContext joltCtx = new JoltHttpContext(
                        context.req,
                        context.res,
                        match.matcher(),
                        match.route().getParamNames());
                RouteHandler handler = match.route().getHandler();
                Object result = handler.handle(joltCtx);

                if (!context.res.isCommitted() && result != null && !(result instanceof JoltHttpContext)) {
                    if (result instanceof String str) {
                        joltCtx.text(str);
                    } else {
                        joltCtx.json(result);
                    }
                }
                long duration = System.currentTimeMillis() - start;
                log.info(() -> context.path + " handled successfully in " + duration + "ms");
                return true;
            } catch (Exception e) {
                log.warning(() -> "Error in route handler: " + e.getMessage());
                exceptionHandler.handle(e, context.res);
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to serve a static resource or handle a not found error.
     *
     * @param context The request context
     * @param start   The start time of request processing
     * @throws IOException if an I/O error occurs during static resource serving
     */
    private void handleStaticResourceOrError(RequestContext context, long start) throws IOException {
        if (context.method.equals("GET")) {
            if (tryServeStaticResource(context.path, context.req, context.res)) {
                long duration = System.currentTimeMillis() - start;
                log.info(() -> "Static resource " + context.path + " served successfully in " + duration + "ms");
                return;
            }
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
    }

    /**
     * Determines the request path by checking
     * {@link HttpServletRequest#getPathInfo()}
     * first, then falling back to {@link HttpServletRequest#getServletPath()} if
     * necessary.
     * If neither is present, returns {@code "/"} as a default.
     *
     * @param req
     *            The {@link HttpServletRequest} from which the path is extracted.
     * @return
     *         The normalized request path, never {@code null}.
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
     * @param req  The HTTP servlet request
     * @param res  The HTTP servlet response
     * @return true if a static resource was found and served, false otherwise
     */
    private boolean tryServeStaticResource(String path, HttpServletRequest req, HttpServletResponse res) {
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

        RequestContext(HttpServletRequest req, HttpServletResponse res, String method, String path) {
            this.req = req;
            this.res = res;
            this.method = method;
            this.path = path;
        }
    }
}