package io.github.t1willi.filters.security;

import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.security.config.NonceConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.nonce.Nonce;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A filter that manages the lifecycle of nonces for Content Security Policy
 * (CSP).
 * <p>
 * This filter generates a nonce at the start of each request (if enabled in
 * SecurityConfiguration and the request URI is not excluded).
 * The nonce is cleared at the end of the request lifecycle by the
 * JoltDispatcherServlet.
 * </p>
 */
@Bean
public class NonceFilter extends JoltFilter {
    private static final Logger logger = Logger.getLogger(NonceFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        logger.fine("NonceFilter running for request: " + req.getRequestURI());
        SecurityConfiguration securityConfig = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class);
        NonceConfiguration nonceConfig = securityConfig.getNonceConfig();

        if (nonceConfig.isEnabled() && !isExcludedUrl(req.getRequestURI(), nonceConfig.getExcludedUrlPatterns())) {
            Nonce.generate();
            logger.fine("Nonce generated for request: " + req.getRequestURI());
        } else {
            logger.fine("Nonce generation skipped for request: " + req.getRequestURI() +
                    (nonceConfig.isEnabled() ? " (URL excluded)" : " (nonce disabled)"));
        }

        chain.doFilter(request, response);
    }

    private boolean isExcludedUrl(String requestUri, Set<String> excludedUrlPatterns) {
        return excludedUrlPatterns.stream().anyMatch(requestUri::matches);
    }
}