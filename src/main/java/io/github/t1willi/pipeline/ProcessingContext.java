package io.github.t1willi.pipeline;

import java.util.List;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.routing.RouteMatch;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ProcessingContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final long startTime;
    private final String method;
    private final String path;
    @Setter
    private RouteMatch match;
    @Setter
    private JoltContext context;

    public ProcessingContext(HttpServletRequest req, HttpServletResponse res, long startTime) {
        this.request = req;
        this.response = res;
        this.context = new JoltContext(req, res, null, List.of());
        this.startTime = startTime;
        this.method = req.getMethod();
        this.path = this.context.requestPath();
    }
}
