package io.github.t1willi.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.exceptions.handler.GlobalExceptionHandler;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.pipeline.AfterStep;
import io.github.t1willi.pipeline.BeforeStep;
import io.github.t1willi.pipeline.FilterStep;
import io.github.t1willi.pipeline.InvocationStep;
import io.github.t1willi.pipeline.ParamBindingStep;
import io.github.t1willi.pipeline.ProcessingContext;
import io.github.t1willi.pipeline.ResponseStep;
import io.github.t1willi.pipeline.RoutePipeline;
import io.github.t1willi.pipeline.RoutingStep;
import io.github.t1willi.pipeline.StaticResourceStep;
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
    private static final ThreadLocal<JoltContext> CURRENT = new ThreadLocal<>();

    private final RoutePipeline pipeline;
    private final GlobalExceptionHandler exceptionHandler;

    /**
     * Retrieves the current JoltContext for handlers.
     */
    public static JoltContext getCurrentContext() {
        return CURRENT.get();
    }

    public JoltDispatcherServlet() {
        this.pipeline = new RoutePipeline(List.of(
                new FilterStep(),
                new BeforeStep(),
                new RoutingStep(),
                new StaticResourceStep(),
                new ParamBindingStep(),
                new InvocationStep(),
                new ResponseStep()));
        this.exceptionHandler = JoltContainer.getInstance().getBean(GlobalExceptionHandler.class);
        this.exceptionHandler.init();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        ProcessingContext ctx = new ProcessingContext(req, res, start);
        CURRENT.set(ctx.getContext());
        boolean errorHandled = false;
        try {
            pipeline.execute(ctx);
        } catch (Exception e) {
            log.warning("Error during request: " + e.getMessage());
            log.fine(() -> "Stack Trace: " + HelpMethods.stackTraceElementToString(e.getStackTrace()));
            exceptionHandler.handleException(e, res);
            errorHandled = true;
        } finally {
            if (!errorHandled) {
                if (!res.isCommitted()) {
                    ctx.getContext().commit();
                }
                new AfterStep().execute(ctx);
            }
            CURRENT.remove();
            logIncoming(req, System.currentTimeMillis() - start);
        }
    }

    private void logIncoming(HttpServletRequest req, long duration) {
        String method = req.getMethod();
        String path = req.getPathInfo() != null ? req.getPathInfo() : req.getServletPath();
        String remote = req.getRemoteAddr();
        log.info(() -> String.format("Incoming %s %s from %s - Handled in %dms", method, path, remote, duration));
    }
}