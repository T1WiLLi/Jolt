package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.core.MvcController;

@Controller()
public class HomeController extends MvcController {

    @Get("/*")
    public String index() {
        return "index.html";
    }

    @Get("/api")
    public String api() {
        return "This is the API endpoint.";
    }
}
