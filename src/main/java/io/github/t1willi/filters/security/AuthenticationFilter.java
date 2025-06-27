package io.github.t1willi.filters.security;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.ControllerRegistry;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.security.authentification.AuthStrategy;
import io.github.t1willi.security.authentification.JWTAuthStrategy;
import io.github.t1willi.security.authentification.RouteRule;
import io.github.t1willi.security.authentification.SessionAuthStrategy;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.utils.JwtToken;
import io.github.t1willi.utils.HelpMethods;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Bean
public class AuthenticationFilter extends JoltFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        JoltContext ctx = buildJoltContext(httpRequest, httpResponse);
        String path = ctx.rawPath();
        String method = httpRequest.getMethod();

        List<RouteRule> rules = getRouteRules();
        for (RouteRule rule : rules) {
            if (!matches(rule, path, method)) {
                continue;
            }
            if (rule.isPermitAll()) {
                chain.doFilter(request, response);
                return;
            }
            if (rule.isDenyAll()) {
                ctx.status(403).contentType("application/x-www-form-urlencoded")
                        .abortUnauthorized("Access denied");
                return;
            }
            if (rule.getStrategy() == null) {
                ctx.status(403).contentType("application/x-www-form-urlencoded")
                        .abortUnauthorized("Authentication required but no strategy defined");
                return;
            }
            if (!authenticate(ctx, rule)) {
                if (!rule.handleFailure(ctx)) {
                    rule.getStrategy().challenge(ctx);
                }
                return;
            }
            if (!authenticateWithCredentials(ctx, rule)) {
                ctx.status(HttpStatus.UNAUTHORIZED).contentType("application/x-www-form-urlencoded")
                        .abortUnauthorized("Authentication failed due to invalid credentials");
                return;
            }
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private List<RouteRule> getRouteRules() {
        List<RouteRule> rules = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getRouteConfig()
                .getRules();
        rules.addAll(ControllerRegistry.AUTHORIZATION);
        return rules;
    }

    private boolean authenticate(JoltContext context, RouteRule rule) {
        AuthStrategy strategy = rule.getStrategy();
        if (strategy.authenticate(context)) {
            return true;
        }
        return false;
    }

    private boolean authenticateWithCredentials(JoltContext ctx, RouteRule rule) {
        AuthStrategy strategy = rule.getStrategy();
        if (!(strategy instanceof JWTAuthStrategy) && !(strategy instanceof SessionAuthStrategy)) {
            return true;
        }
        Map<String, Object> credentials = rule.getCredentials();
        return validateCredentials(ctx, strategy, credentials);
    }

    private boolean validateCredentials(JoltContext ctx, AuthStrategy strategy, Map<String, Object> credentials) {
        if (credentials.isEmpty()) {
            return true;
        }
        Map<String, Object> authData = getAuthData(ctx, strategy, credentials.keySet());
        if (authData == null) {
            return false;
        }
        for (Map.Entry<String, Object> entry : credentials.entrySet()) {
            String key = entry.getKey();
            Object expected = entry.getValue();
            Object actual = authData.get(key);
            if (!HelpMethods.equivalentValues(actual, expected)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> getAuthData(JoltContext ctx, AuthStrategy strategy, Set<String> keys) {
        if (strategy instanceof JWTAuthStrategy) {
            return ctx.bearerToken()
                    .map(JwtToken::getClaims)
                    .orElse(null);
        } else if (strategy instanceof SessionAuthStrategy) {
            return SessionAuthStrategy.getSessionAttributes(keys);
        }
        return null;
    }

    private boolean matches(RouteRule rule, String path, String method) {
        if (rule.getMethods() != null && !rule.getMethods().contains(method)) {
            return false;
        }
        if (rule.isAny()) {
            return true;
        }
        String regex = buildRegex(rule.getPattern());
        return path.matches(regex);
    }

    private String buildRegex(String dsl) {
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
                if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                    sb.append('\\');
                }
                sb.append(c);
            }
        }
        return "^" + sb + "$";
    }
}