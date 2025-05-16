package io.github.t1willi.pipeline;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import io.github.t1willi.filters.FilterConfiguration;
import io.github.t1willi.filters.JoltFilter;
import io.github.t1willi.injector.JoltContainer;
import jakarta.servlet.ServletException;

public final class FilterStep implements PipelineStep {
    private final FilterConfiguration config;
    private final List<JoltFilter> filters;

    public FilterStep() {
        this.config = JoltContainer.getInstance().getBean(FilterConfiguration.class);
        this.filters = JoltContainer.getInstance().getBeans(JoltFilter.class)
                .stream()
                .sorted(Comparator.comparingInt(config::getOrder))
                .toList();
    }

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        for (JoltFilter filter : filters) {
            if (config.shouldExcludeRoute(context.getContext())) {
                continue;
            }
            filter.doFilter(context.getRequest(), context.getResponse(), (req, res) -> {
            });
            if (context.getResponse().isCommitted()) {
                return true;
            }
        }
        return false;
    }
}