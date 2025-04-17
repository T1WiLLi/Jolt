package io.github.t1willi;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.routing.context.JoltContext;

@Controller
public class HomeController extends BaseController {

    @Get()
    public JoltContext home(JoltContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head><title>Login Panel</title></head>");
        sb.append("<body>");
        sb.append("<h2>Login Panel</h2>");
        sb.append("<form method='post' action='/login'>");
        sb.append("  <label for='username'>Username:</label><br>");
        sb.append("  <input type='text' id='username' name='username'><br>");
        sb.append("  <label for='password'>Password:</label><br>");
        sb.append("  <input type='password' id='password' name='password'><br><br>");
        sb.append("  <input type='submit' value='Login'>");
        sb.append("</form>");
        sb.append("</body>");
        sb.append("</html>");

        ctx.html(sb.toString());
        return ctx;
    }
}
