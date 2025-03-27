package io.github.t1willi.filters.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.security.config.CorsConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;

/**
 * A filter that enforces Cross-Origin Resource Sharing (CORS) rules.
 * <p>
 * This filter obtains the CORS configuration from the container's
 * {@link SecurityConfiguration}, verifies that the request method is allowed,
 * and sets appropriate {@code Access-Control} headers on the response.
 * If the request method is not allowed, a {@link JoltHttpException} with
 * an HTTP 405 status is thrown.
 * </p>
 */
@JoltBean
public final class CorsFilter extends JoltFilter {

        /**
         * Processes incoming requests for CORS compliance.
         * <p>
         * Retrieves the configured CORS settings, verifies the request method against
         * the allowed methods, and sets the necessary CORS response headers. If the
         * request method is disallowed, an exception is thrown with status
         * {@code METHOD_NOT_ALLOWED}.
         * </p>
         *
         * @param request  The incoming {@link ServletRequest}
         * @param response The outgoing {@link ServletResponse}
         * @param chain    The {@link FilterChain} to pass the request and response
         * @throws IOException      If an I/O error occurs during filter processing
         * @throws ServletException If a servlet-related error occurs during filtering
         */
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                        throws IOException, ServletException {

                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                CorsConfiguration cors = JoltContainer.getInstance()
                                .getBean(SecurityConfiguration.class)
                                .getCorsConfig();

                Set<String> allowedMethods = Arrays.stream(cors.getAllowedMethods().split(",\\s*"))
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet());

                String requestMethod = req.getMethod().toUpperCase();

                if (!allowedMethods.contains(requestMethod)) {
                        throw new JoltHttpException(HttpStatus.METHOD_NOT_ALLOWED,
                                        "The method " + requestMethod + " is not allowed.");
                }

                res.setHeader("Access-Control-Allow-Origin", cors.getAllowedOrigins());
                res.setHeader("Access-Control-Allow-Methods", cors.getAllowedMethods());
                res.setHeader("Access-Control-Allow-Headers", cors.getAllowedHeaders());
                res.setHeader("Access-Control-Allow-Credentials", Boolean.toString(cors.isAllowCredentials()));

                chain.doFilter(request, response);
        }
}
