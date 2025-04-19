package io.github.t1willi.template;

import lombok.Getter;

@Getter
public final class Template {
    private final String view;
    private final JoltModel model;

    public Template(String view) {
        this.view = view;
        this.model = JoltModel.create();
    }

    public Template(String view, JoltModel model) {
        this.view = view;
        this.model = model;
    }
}
