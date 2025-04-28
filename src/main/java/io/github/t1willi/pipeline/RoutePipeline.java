package io.github.t1willi.pipeline;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;

public class RoutePipeline {
    private final List<PipelineStep> steps;

    public RoutePipeline(List<PipelineStep> steps) {
        this.steps = steps;
    }

    public void execute(ProcessingContext context) throws IOException, ServletException {
        for (PipelineStep step : steps) {
            if (step.execute(context)) {
                break;
            }
        }
    }
}
