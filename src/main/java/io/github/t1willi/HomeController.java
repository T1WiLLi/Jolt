package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Root;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.routing.context.JoltContext;

@Controller
@Root("[controller]")
public class HomeController extends BaseController {

    @Get
    public JoltContext home(JoltContext context) {
        return context.html("Hello Bitches");
    }
}
