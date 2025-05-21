package io.github.t1willi.http;

import io.github.t1willi.template.JoltModel;
import lombok.Getter;

@Getter
public final class ModelView {
    private final String view;
    private final JoltModel model;

    private ModelView(String view, JoltModel model) {
        this.view = view;
        this.model = model != null ? model : JoltModel.empty();
    }

    public static ModelView of(String view, JoltModel model) {
        return new ModelView(view, model);
    }
}
