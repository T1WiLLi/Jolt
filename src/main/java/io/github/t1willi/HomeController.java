package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.core.MvcController;
import io.github.t1willi.http.ModelView;
import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.template.JoltModel;

@Controller("[controller]")
public class HomeController extends MvcController {

    @Get
    public ResponseEntity<ModelView> index() {
        JoltModel model = JoltModel.of("msg", "Welcome to Jolt MVC!");
        return render("home", model);
    }

    @Get("/plain")
    public ResponseEntity<String> plain() {
        return ResponseEntity.ok("Hello, world!");
    }

    @Get("/redirect")
    public ResponseEntity<Void> goHome() {
        return redirect("/home");
    }
}
