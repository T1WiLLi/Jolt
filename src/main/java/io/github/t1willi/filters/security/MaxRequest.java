package io.github.t1willi.filters.security;

import java.io.IOException;

import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.injector.annotation.JoltBean;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@JoltBean
public class MaxRequest extends JoltFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

    }
}
