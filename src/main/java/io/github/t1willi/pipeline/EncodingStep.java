package io.github.t1willi.pipeline;

import java.io.IOException;

import jakarta.servlet.ServletException;

public class EncodingStep implements PipelineStep {

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        context.getRequest().setCharacterEncoding("UTF-8");
        context.getResponse().setCharacterEncoding("UTF-8");
        return false;
    }
}
