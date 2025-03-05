package ca.jolt.filters.security;

import ca.jolt.injector.JoltContainer;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.security.config.CorsConfiguration;
import ca.jolt.security.config.SecurityConfiguration;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.filters.JoltFilter;
import ca.jolt.http.HttpStatus;
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

@JoltBean
public final class CorsFilter extends JoltFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        CorsConfiguration cors = JoltContainer.getInstance().getBean(SecurityConfiguration.class).getCorsConfig();

        Set<String> allowedMethods = Arrays.stream(cors.getAllowedMethods().split(",\\s*"))
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        allowedMethods.forEach(s -> System.out.println(s));
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