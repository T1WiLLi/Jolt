package io.github.t1willi.pipeline;

import java.io.IOException;

import io.github.t1willi.context.JoltContext;
import jakarta.servlet.ServletException;

public class ParamBindingStep implements PipelineStep {
    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        if (context.getMatch() != null) {
            context.setContext(new JoltContext(
                    context.getRequest(),
                    context.getResponse(),
                    context.getMatch(),
                    context.getMatch().route().getParamNames()));
        }
        return false;
    }
}
