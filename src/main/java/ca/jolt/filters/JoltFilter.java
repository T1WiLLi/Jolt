package ca.jolt.filters;

import ca.jolt.routing.context.JoltContext;
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
 * Serves as a base class for Jolt filters, implementing the standard Jakarta
 * Servlet {@link Filter} interface. Subclasses should override
 * {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} to implement
 * custom filter logic.
 */
public abstract class JoltFilter implements Filter {

    /**
     * Called by the servlet container to initialize this filter. Default
     * implementation does nothing.
     *
     * @param filterConfig The filter configuration object
     * @throws ServletException If an error occurs during initialization
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Default initialization, if needed
    }

    /**
     * Called by the servlet container to indicate that the filter is being taken
     * out of service. Default implementation does nothing.
     */
    @Override
    public void destroy() {
        // Default cleanup, if needed
    }

    /**
     * Checks if the current request path matches any of the specified routes.
     * If there is no match, this method delegates to the next filter in the
     * chain and returns {@code false}.
     *
     * @param request  The current {@link ServletRequest}
     * @param response The current {@link ServletResponse}
     * @param chain    The {@link FilterChain} for further processing
     * @param routes   One or more route paths (e.g., {@code "/doc"},
     *                 {@code "/api"})
     * @return {@code true} if the request path matches at least one route;
     *         otherwise {@code false}
     * @throws IOException      If an I/O error occurs
     * @throws ServletException If a servlet error occurs
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
     * Creates a {@link JoltContext} for the current request and response.
     * This context is built with an empty list of path parameters.
     *
     * @param req The current {@link HttpServletRequest}
     * @param res The current {@link HttpServletResponse}
     * @return A newly created {@link JoltContext} for this request
     */
    protected JoltContext buildJoltContext(ServletRequest req, ServletResponse res) {
        return new JoltContext((HttpServletRequest) req, (HttpServletResponse) res, null, Collections.emptyList());
    }
}