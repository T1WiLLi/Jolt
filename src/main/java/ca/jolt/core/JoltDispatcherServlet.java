package ca.jolt.core;

import ca.jolt.exceptions.JoltHttpException;
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
    private final boolean useCustomNotFound;
    private final boolean useCustomError;

    public JoltDispatcherServlet(Router router,
            boolean useCustomNotFound,
            boolean useCustomError) {
        this.router = router;
        this.useCustomNotFound = useCustomNotFound;
        this.useCustomError = useCustomError;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        String method = req.getMethod();
        String path = getPath(req);

        log.info("Incoming " + req.getMethod() + " " + req.getRequestURI());

        RouteMatch match = router.match(method, path);
        if (match == null) {
            handleNotFound(res, path);
            return;
        }

        JoltHttpContext joltCtx = new JoltHttpContext(
                req, res, match.matcher(), match.route().getParamNames());
        RouteHandler handler = match.route().getHandler();

        try {
            Object result = handler.handle(joltCtx);
            if (!res.isCommitted() && result != null && !(result instanceof JoltHttpContext)) {
                if (result instanceof String) {
                    joltCtx.text((String) result);
                } else {
                    joltCtx.json(result);
                }
            }
            log.info(path + " handled successfully in " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            log.severe("Error in route handler: " + e.getMessage());
            handleError(res, e);
        }
    }

    private void handleNotFound(HttpServletResponse res, String path) throws IOException {
        log.info("No route matched for path: " + path);
        if (useCustomNotFound) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.setContentType("text/plain");
            res.getWriter().write("404 - Not Found (Custom)");
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleError(HttpServletResponse res, Exception e) throws IOException {
        if (e instanceof JoltHttpException) {
            JoltHttpException jhe = (JoltHttpException) e;
            log.warning("Custom HTTP exception: " + jhe.getStatus() + " - " + jhe.getMessage());
            if (useCustomError) {
                res.setStatus(jhe.getStatus().code());
                res.setContentType("text/plain");
                res.getWriter().write(jhe.getMessage());
            } else {
                res.sendError(jhe.getStatus().code(), jhe.getMessage());
            }
        } else {
            if (useCustomError) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.setContentType("text/plain");
                res.getWriter().write("500 - Internal Server Error (Custom)\n" + e.getMessage());
            } else {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    private String getPath(HttpServletRequest req) {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }
}