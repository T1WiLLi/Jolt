package io.github.t1willi.filters.security;

import io.github.t1willi.exceptions.CsrfTokenException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.security.config.CsrfConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.csrf.CsrfHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * A filter that enforces CSRF protection for modifying HTTP methods.
 * Skips all logic if CSRF protection is disabled in the configuration.
 */
@JoltBean
public final class CsrfFilter extends JoltFilter {
    private static final Logger logger = Logger.getLogger(CsrfFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        JoltContext context = new JoltContext(req, res, null, Collections.emptyList());

        CsrfConfiguration config = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getCsrfConfig();
        if (!config.isEnabled()) {
            logger.fine(() -> "CSRF protection is disabled, skipping filter logic");
            chain.doFilter(request, response);
            return;
        }

        try {
            CsrfHandler handler = config.getHandler();
            handler.validate(context, config);
            chain.doFilter(request, response);
        } catch (JoltHttpException e) {
            logger.warning(() -> "CSRF validation failed: " + e.getMessage());
            throw new CsrfTokenException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            logger.severe(() -> "Error validating CSRF token: " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error validating CSRF token");
        }
    }
}