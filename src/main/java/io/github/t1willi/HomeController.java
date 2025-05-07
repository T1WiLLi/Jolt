package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Path;
import io.github.t1willi.annotations.Query;
import io.github.t1willi.annotations.Version;
import io.github.t1willi.core.BaseController;

@Controller("api/[controller]")
@Version(2)
public class HomeController extends BaseController {

    @Get
    public String home() {
        return "Hello World from a very nice version";
    }

    @Get
    @Version(value = 3, prefix = "ver")
    public String homeV3() {
        return "This a way better version, we are touching V3";
    }

    @Get("/user")
    public String user(@Query String name) {
        return "Hello " + name;
    }

    @Get("/user/{name}")
    public String userPath(@Path String name) {
        return "Hello " + name;
    }
}
