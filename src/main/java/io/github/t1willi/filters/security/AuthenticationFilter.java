package io.github.t1willi.filters.security;

import java.io.IOException;
import java.util.List;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.ControllerRegistry;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.security.authentification.RouteRule;
import io.github.t1willi.security.config.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@JoltBean
public class AuthenticationFilter extends JoltFilter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        JoltContext ctx = buildJoltContext(httpRequest, httpResponse);
        String path = ctx.requestPath();
        String method = httpRequest.getMethod();

        List<RouteRule> rules = JoltContainer.getInstance().getBean(SecurityConfiguration.class).getRouteConfig()
                .getRules();

        rules.addAll(ControllerRegistry.AUTHORIZATION);

        for (RouteRule r : rules) {
            if (matches(r, path, method)) {
                if (r.isPermitAll()) {
                    chain.doFilter(request, response);
                    return;
                }
                if (r.isDenyAll()) {
                    ctx.abortUnauthorized("Access denied");
                    return;
                }

                if (r.getStrategy() != null && r.getStrategy().authenticate(ctx)) {
                    chain.doFilter(request, response);
                } else if (r.getStrategy() != null) {
                    r.getStrategy().challenge(ctx);
                } else {
                    ctx.abortUnauthorized("Authentication required but no strategy defined");
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean matches(RouteRule r, String path, String method) {
        boolean pathMatch = r.isAny() || (r.getPattern().endsWith("/**")
                ? path.startsWith(r.getPattern().substring(0, r.getPattern().length() - 3))
                : path.equals(r.getPattern()));
        boolean methodMatch = r.getMethods() == null || r.getMethods().contains(method);
        return pathMatch && methodMatch;
    }
}
