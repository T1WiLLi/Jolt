package ca.jolt;

import java.io.IOException;

import ca.jolt.filters.JoltFilter;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.routing.context.JoltHttpContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@JoltBean
public class LoggingFilter extends JoltFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (forRoutes(request, response, chain, "/", "/doc")) {
            JoltHttpContext ctx = buildJoltContext(request, response);
            System.out.println("Filter: Processing request for: " + ctx.requestPath());
        }
        chain.doFilter(request, response);
    }
}
