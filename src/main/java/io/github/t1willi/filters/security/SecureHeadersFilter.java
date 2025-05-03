package io.github.t1willi.filters.security;

import java.io.IOException;
import java.util.logging.Logger;

import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.security.config.HeadersConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.nonce.Nonce;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Applies various security-related HTTP response headers.
 * <p>
 * This filter retrieves header configuration from the Jolt
 * {@link SecurityConfiguration} and sets headers such as
 * <strong>Content-Security-Policy</strong>, <strong>X-XSS-Protection</strong>,
 * <strong>X-Frame-Options</strong>, <strong>Strict-Transport-Security</strong>,
 * and others. If {@code httpsOnly} is enabled and the request is not secure,
 * the filter redirects to an HTTPS URL.
 */
@Bean
public final class SecureHeadersFilter extends JoltFilter {
    private static final Logger logger = Logger.getLogger(SecureHeadersFilter.class.getName());

    /**
     * Sets security headers on the HTTP response and optionally enforces HTTPS.
     * <p>
     * If {@code httpsOnly} is enabled and the request is not secure,
     * the request is redirected to the HTTPS version of the URL.
     *
     * @param request  The current {@link ServletRequest}
     * @param response The current {@link ServletResponse}
     * @param chain    The filter chain
     * @throws IOException      If an I/O error occurs during filtering
     * @throws ServletException If a servlet-related error is detected
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HeadersConfiguration headers = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getHeadersConfig();

        if (headers.isContentSecurityPolicyEnabled()) {
            String csp = headers.getContentSecurityPolicy();
            String nonce = Nonce.get();
            if (nonce != null) {
                csp = csp.replace("'nonce-{{NONCE}}'", "'nonce-" + nonce + "'");
                logger.fine("CSP header set with nonce: " + nonce);
            } else {
                csp = csp.replace("'nonce-{{NONCE}}'", "");
                logger.fine("CSP header set without nonce because no nonce was available");
            }
            res.setHeader("Content-Security-Policy", csp);
        }
        if (headers.isXssProtectionEnabled()) {
            res.setHeader("X-XSS-Protection", headers.getXssProtectionValue());
        }
        if (headers.isFrameOptionsEnabled()) {
            res.setHeader("X-Frame-Options", headers.getFrameOptionsValue());
        }
        if (headers.isHstsEnabled()) {
            res.setHeader("Strict-Transport-Security", headers.getHstsValue());
        }

        res.setHeader("Referrer-Policy", headers.getReferrerPolicy());
        res.setHeader("X-Content-Type-Options", "nosniff");

        if (headers.getCacheControlDirective() != null) {
            res.setHeader("Cache-Control", headers.getCacheControlDirective());
        }

        if (headers.isHttpsOnly() && !req.isSecure()) {
            res.sendRedirect("https://" + req.getServerName() + req.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }
}
