package io.github.t1willi;

import java.util.List;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Version;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.injector.annotation.Autowire;

@Controller("[controller]")
public class HomeController extends BaseController {

    @Autowire
    private UserService userService;

    @Get // '/home'
    public String index() {
        return "<h1>Allo Oualid</h1>";
    }

    @Version(2)
    public String indexV2() {
        return "<h1>Fuck you Oualid</h1>";
    }

    @Get("/users")
    public List<User> getUsers() {
        return userService.create();
    }
}