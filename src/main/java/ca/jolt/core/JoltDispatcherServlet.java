package ca.jolt.core;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.JoltContainer;
import ca.jolt.routing.MimeInterpreter;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;
import ca.jolt.routing.context.JoltHttpContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * The {@code JoltDispatcherServlet} is the main servlet responsible for
 * dispatching
 * incoming HTTP requests to the appropriate route handler in a Jolt-based
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
        String method = req.getMethod();
        String path = getPath(req);

        log.info(() -> String.format("Incoming %s %s", method, path));

        RouteMatch match = router.match(method, path);

        if (match != null) {
            try {
                JoltHttpContext joltCtx = new JoltHttpContext(req, res, match.matcher(), match.route().getParamNames());
                RouteHandler handler = match.route().getHandler();
                Object result = handler.handle(joltCtx);

                if (!res.isCommitted() && result != null && !(result instanceof JoltHttpContext)) {
                    if (result instanceof String str) {
                        joltCtx.text(str);
                    } else {
                        joltCtx.json(result);
                    }
                }
                long duration = System.currentTimeMillis() - start;
                log.info(() -> path + " handled successfully in " + duration + "ms");
                return;
            } catch (Exception e) {
                log.warning(() -> "Error in route handler: " + e.getMessage());
                exceptionHandler.handle(e, res);
                return;
            }
        }

        // No dynamic route found – if GET, try to serve a static resource.
        if (method.equals("GET")) {
            if (tryServeStaticResource(path, req, res)) {
                long duration = System.currentTimeMillis() - start;
                log.info(() -> "Static resource " + path + " served successfully in " + duration + "ms");
                return;
            }
        }

        List<String> allowedMethods = router.getAllowedMethods(path);
        if (!allowedMethods.isEmpty()) {
            log.info(() -> "HTTP method " + method + " is not allowed for path: " + path);
            throw new JoltHttpException(HttpStatus.METHOD_NOT_ALLOWED,
                    "HTTP method " + method + " not allowed for " + path);
        }

        log.info(() -> "No route found for path: " + path);
        throw new JoltHttpException(HttpStatus.NOT_FOUND, "No route found for " + path);
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
            // Handle paths that might start with a slash
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

            // First try exact path
            InputStream in = getClass().getClassLoader().getResourceAsStream("static/" + normalizedPath);

            // If not found and looks like root request, try index.html
            if (in == null && (normalizedPath.isEmpty() || normalizedPath.equals("/"))) {
                normalizedPath = "index.html";
                in = getClass().getClassLoader().getResourceAsStream("static/" + normalizedPath);
            }

            // If resource found, serve it
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
}