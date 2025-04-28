package io.github.t1willi.pipeline;

import jakarta.servlet.ServletException;
import java.io.IOException;

public class ResponseStep implements PipelineStep {
    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        if (!context.getResponse().isCommitted()) {
            context.getContext().commit();
        }
        return false;
    }
}
