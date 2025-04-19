package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.RequestForm;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.form.Form;
import io.github.t1willi.injector.annotation.JoltBeanInjection;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.Template;

@Controller
public class HomeController extends BaseController {

    @JoltBeanInjection
    private MyService myService;

    @Get("/")
    public String index() {
        return "Hello, World!";
    }

    @Get("/home")
    public Template home() {
        return new Template("home", new JoltModel().with("items", myService.getList()));
    }

    @Post("/add")
    public Template addItem(@RequestForm Form form) {
        String newItem = form.getValue("item");
        myService.addItem(newItem);
        return new Template("home", new JoltModel().with("items", myService.getList()));
    }
}