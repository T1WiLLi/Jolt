package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.security.authentification.Authorize;
import io.github.t1willi.security.session.Session;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.Template;

@Controller
public class HomeController extends BaseController {

    @Get("/dashboard")
    @Authorize
    public Template dashboard() {
        return new Template("dashboard", JoltModel.of("user", Session.get("user", "Guest")));
    }
}
