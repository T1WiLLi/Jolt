package io.github.t1willi.pipeline;

import java.io.IOException;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.exceptions.JoltRoutingException;
import jakarta.servlet.ServletException;

public class InvocationStep implements PipelineStep {

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        if (context.getMatch() == null) {
            return false;
        }

        try {
            Object result = context.getMatch().route().getHandler().handle(context.getContext());
            if (!context.getResponse().isCommitted() && result != null && !(result instanceof JoltContext)) {
                if (result instanceof String str) {
                    context.getContext().text(str);
                } else {
                    context.getContext().json(result);
                }
            }
            return false;
        } catch (JoltRoutingException e) {
            throw e;
        }
    }
}
