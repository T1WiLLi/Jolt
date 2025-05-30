package io.github.t1willi.filters.security;

import java.io.IOException;
import java.util.List;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.ControllerRegistry;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.security.authentification.RouteRule;
import io.github.t1willi.security.config.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Bean
public class AuthenticationFilter extends JoltFilter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        JoltContext ctx = buildJoltContext(httpRequest, httpResponse);
        String path = ctx.rawPath();
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

                if (r.getStrategy() == null) {
                    ctx.abortUnauthorized("Authentication required but no strategy defined");
                    return;
                }
                if (r.getStrategy().authenticate(ctx)) {
                    chain.doFilter(request, response);
                    return;
                }
                if (!r.handleFailure(ctx)) {
                    r.getStrategy().challenge(ctx);
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean matches(RouteRule r, String path, String method) {
        if (r.getMethods() != null && !r.getMethods().contains(method)) {
            return false;
        }
        if (r.isAny()) {
            return true;
        }
        String dsl = r.getPattern();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dsl.length();) {
            if (i + 1 < dsl.length() && dsl.charAt(i) == '*' && dsl.charAt(i + 1) == '*') {
                sb.append(".*");
                i += 2;
            } else if (dsl.charAt(i) == '*') {
                sb.append("[^/]+");
                i++;
            } else {
                char c = dsl.charAt(i++);
                if ("\\.[]{}()+-^$|".indexOf(c) >= 0)
                    sb.append('\\');
                sb.append(c);
            }
        }
        String regex = "^" + sb + "$";
        return path.matches(regex);
    }
}
