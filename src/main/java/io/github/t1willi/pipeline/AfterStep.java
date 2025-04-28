package io.github.t1willi.pipeline;

import java.io.IOException;

import io.github.t1willi.context.LifecycleEntry;
import io.github.t1willi.core.Router;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.security.nonce.Nonce;
import jakarta.servlet.ServletException;

public class AfterStep implements PipelineStep {
    private final Router router;

    public AfterStep() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
    }

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        for (LifecycleEntry entry : router.getAfterHandlers()) {
            if (entry.matches(context.getContext().requestPath())) {
                entry.execute(context.getContext());
            }
        }
        Nonce.clear();
        return false;
    }
}
