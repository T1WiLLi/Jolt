package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.core.BaseController;

@Controller
public class HomeController extends BaseController {

    @Get
    public String index() {
        return "index.html";
    }
}
