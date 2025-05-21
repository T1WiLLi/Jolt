package io.github.t1willi.core;

import io.github.t1willi.http.ModelView;
import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.template.JoltModel;

public abstract class MvcController extends BaseController {

    protected ResponseEntity<ModelView> render(String view, JoltModel model) {
        return ResponseEntity.ok(ModelView.of(view, model))
                .header("Content-Type", "text/html");
    }

    protected ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.redirect(location);
    }

    protected ResponseEntity<JoltModel> redirect(String location, JoltModel model) {
        return ResponseEntity.redirect(location, model)
                .header("Content-Type", "text/html");
    }
}