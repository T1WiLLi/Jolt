package ca.jolt.core;

import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.routing.JoltHttpContext;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public final class JoltDispatcherServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(JoltDispatcherServlet.class.getName());

    private final Router router;
    private final GlobalExceptionHandler exceptionHandler;

    public JoltDispatcherServlet(Router router, GlobalExceptionHandler exceptionHandler) {
        this.router = router;
        this.exceptionHandler = (exceptionHandler != null) ? exceptionHandler : GlobalExceptionHandler.defaultHandler();

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = req.getMethod();
        String path = getPath(req);

        log.info(() -> String.format("Incoming %s %s", method, path));

        RouteMatch match = router.match(method, path);
        if (match == null) {
            log.info("No route matched for path: " + path);
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        JoltHttpContext joltCtx = new JoltHttpContext(
                req, res, match.matcher(), match.route().getParamNames());
        RouteHandler handler = match.route().getHandler();

        try {
            Object result = handler.handle(joltCtx);

            if (!res.isCommitted() && result != null && !(result instanceof JoltHttpContext)) {
                if (result instanceof String str) {
                    joltCtx.text(str);
                } else {
                    joltCtx.json(result);
                }
            }

            long duration = System.currentTimeMillis() - start;
            log.info(path + " handled successfully in " + duration + "ms");

        } catch (Exception e) {
            log.severe("Error in route handler: " + e.getMessage());
            exceptionHandler.handle(e, res);
        }
    }

    private String getPath(HttpServletRequest req) {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }
}