package ca.jolt.filters;

import ca.jolt.routing.context.JoltHttpContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Base class for Jolt filters. Extend this class to implement your custom
 * filter logic.
 * This class implements the standard Jakarta Servlet Filter interface.
 */
public abstract class JoltFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Default initialization, if needed.
    }

    @Override
    public void destroy() {
        // Default cleanup, if needed.
    }

    /**
     * Checks if the current request's path matches any of the specified routes.
     * If it does not match, automatically calls
     * {@code chain.doFilter(request, response)}
     * and returns false.
     *
     * @param request  the ServletRequest
     * @param response the ServletResponse
     * @param chain    the FilterChain to delegate to if the route doesn't match
     * @param routes   one or more routes to check against (e.g. "/doc", "/api")
     * @return true if the request matches at least one of the routes, false
     *         otherwise.
     * @throws IOException
     * @throws ServletException
     */
    protected boolean forRoutes(ServletRequest request, ServletResponse response, FilterChain chain, String... routes)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return false;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getPathInfo() != null ? req.getPathInfo() : req.getServletPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        for (String r : routes) {
            if (path.equals(r)) {
                return true;
            }
        }
        // No match: delegate immediately to the next filter.
        chain.doFilter(request, response);
        return false;
    }

    /**
     * Convenience method to create a JoltHttpContext for the current
     * request/response.
     * This context is built with an empty list of path parameters.
     *
     * @param req the HttpServletRequest
     * @param res the HttpServletResponse
     * @return a new JoltHttpContext instance.
     */
    protected JoltHttpContext buildJoltContext(ServletRequest req, ServletResponse res) {
        return new JoltHttpContext((HttpServletRequest) req, (HttpServletResponse) res, null, Collections.emptyList());
    }

    /**
     * Implement your filter logic here.
     *
     * @param request  the ServletRequest.
     * @param response the ServletResponse.
     * @param chain    the FilterChain.
     * @throws IOException      if an I/O error occurs.
     * @throws ServletException if a servlet error occurs.
     */
    @Override
    public abstract void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;
}