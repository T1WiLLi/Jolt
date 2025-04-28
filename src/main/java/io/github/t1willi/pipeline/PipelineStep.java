package io.github.t1willi.pipeline;

import java.io.IOException;

import jakarta.servlet.ServletException;

public interface PipelineStep {
    boolean execute(ProcessingContext context) throws IOException, ServletException;
}
