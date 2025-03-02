package ca.jolt.core;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.JoltContainer;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;
import ca.jolt.routing.context.JoltHttpContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public final class JoltDispatcherServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(JoltDispatcherServlet.class.getName());

    private final transient Router router;
    private final transient GlobalExceptionHandler exceptionHandler;

    public JoltDispatcherServlet() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
        this.exceptionHandler = JoltContainer.getInstance().getBean(GlobalExceptionHandler.class);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = req.getMethod();
        String path = getPath(req);

        log.info(() -> String.format("Incoming %s %s", method, path));

        RouteMatch match = router.match(method, path);
        try {
            if (match == null) {
                List<String> allowedMethods = router.getAllowedMethods(path);
                if (!allowedMethods.isEmpty()) {
                    log.info(() -> "HTTP method " + method + " is not allowed for path: " + path);
                    throw new JoltHttpException(HttpStatus.METHOD_NOT_ALLOWED,
                            "HTTP method " + method + " not allowed for " + path);
                }
                log.info(() -> "No route matched for path: " + path);
                throw new JoltHttpException(HttpStatus.NOT_FOUND, "No route found for " + path);
            }

            JoltHttpContext joltCtx = new JoltHttpContext(
                    req, res, match.matcher(), match.route().getParamNames());
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

        } catch (Exception e) {
            log.warning(() -> "Error in route handler: " + e.getMessage());
            exceptionHandler.handle(e, res);
        }
    }

    private String getPath(HttpServletRequest req) {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }
}