package io.github.t1willi.filters.security;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

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

/**
 * A filter that enforces CSRF protection for modifying HTTP methods.
 * <p>
 * This filter retrives the CSRF configuration and handler from the
 * {@link SecurityConfiguration},
 * validates the CSRF token for modifying methods (POST, PUT, DELETE, PATCH),
 * and thorws a
 * {@link JoltHttpException} with HTTP 403 status if validation fails.
 * 
 * <p>
 * The filter can be disabled or configured to ignore specific URL patterns via
 * the {@link CsrfConfiguration} which is configure through a custom
 * implementation of {@link SecurityConfiguration}.
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

        try {
            CsrfConfiguration config = JoltContainer.getInstance().getBean(SecurityConfiguration.class).getCsrfConfig();
            CsrfHandler handler = config.getHandler();
            handler.validate(context, config);
            chain.doFilter(request, response);
        } catch (JoltHttpException e) {
            logger.warning(() -> "CSRF validation failed: " + e.getMessage());
            throw new JoltHttpException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            logger.severe(() -> "Error validating CSRF token");
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error validating CSRF token");
        }
    }
}
