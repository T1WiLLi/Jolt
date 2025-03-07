package ca.jolt.filters.security;

import java.io.IOException;

import ca.jolt.filters.JoltFilter;
import ca.jolt.injector.JoltContainer;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.security.config.HeadersConfiguration;
import ca.jolt.security.config.SecurityConfiguration;
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
@JoltBean
public final class SecureHeadersFilter extends JoltFilter {

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
            res.setHeader("Content-Security-Policy", headers.getContentSecurityPolicy());
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

        if (headers.isHttpsOnly() && !req.isSecure()) {
            res.sendRedirect("https://" + req.getServerName() + req.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }
}
