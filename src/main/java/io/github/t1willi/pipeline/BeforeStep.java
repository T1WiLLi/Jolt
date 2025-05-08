package io.github.t1willi.pipeline;

import java.io.IOException;

import io.github.t1willi.context.LifecycleEntry;
import io.github.t1willi.core.Router;
import io.github.t1willi.injector.JoltContainer;
import jakarta.servlet.ServletException;

public class BeforeStep implements PipelineStep {
    private final Router router;

    public BeforeStep() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
    }

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        for (LifecycleEntry entry : router.getBeforeHandlers()) {
            if (entry.matches(context.getContext().rawPath())) {
                entry.execute(context.getContext());
            }
        }
        return false;
    }
}
