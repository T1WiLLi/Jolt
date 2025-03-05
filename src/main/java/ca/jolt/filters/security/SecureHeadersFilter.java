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

@JoltBean
public final class SecureHeadersFilter extends JoltFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) response;
        HttpServletRequest req = (HttpServletRequest) request;

        HeadersConfiguration headers = JoltContainer.getInstance().getBean(SecurityConfiguration.class)
                .getHeadersConfig();

        if (headers.isContentSecurityPolicyEnabled()) {
            res.setHeader("Content-Security-Policy", headers.getContentSecurityPolicy());
        }

        if (headers.isXssProtectionEnabled()) {
            res.setHeader("X-XSS-Protection", "1; mode=block");
        }

        if (headers.isFrameOptionsEnabled()) {
            res.setHeader("X-Frame-Options", "DENY");
        }

        if (headers.isHstsEnabled()) {
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
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