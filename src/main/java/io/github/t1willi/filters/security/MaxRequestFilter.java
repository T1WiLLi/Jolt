package io.github.t1willi.filters.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.security.config.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@JoltBean
public final class MaxRequestFilter extends JoltFilter {

    private final Map<String, RequestTracker> requestTrackers = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        int maxRequest = JoltContainer.getInstance().getBean(SecurityConfiguration.class).getMaxRequest();
        long timeWindow = 1000; // 1 second
        String clientIp = buildJoltContext(request, response).clientIp();

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        RequestTracker tracker = requestTrackers.computeIfAbsent(clientIp,
                k -> new RequestTracker(maxRequest, timeWindow));
        if (tracker.allowRequest()) {
            chain.doFilter(req, res);
        } else {
            throw new JoltHttpException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
        }
    }

    private static class RequestTracker {
        private final int maxRequests;
        private final long timeWindowMillis;
        private final long[] requestTimestamps;
        private int currIndex = 0;

        public RequestTracker(int maxRequests, long timeWindowMillis) {
            this.maxRequests = maxRequests;
            this.timeWindowMillis = timeWindowMillis;
            this.requestTimestamps = new long[maxRequests];
        }

        /**
         * Checks if a new request is allowed
         * 
         * @return true if a new request is allowed, false otherwise
         */
        public synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();

            int requestWithin = 0;
            for (long timestamp : requestTimestamps) {
                if (currentTime - timestamp < timeWindowMillis) {
                    requestWithin++;
                }
            }

            if (requestWithin < maxRequests) {
                requestTimestamps[currIndex] = currentTime;
                currIndex = (currIndex + 1) % maxRequests;
                return true;
            }
            return false;
        }
    }
}
