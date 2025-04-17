package io.github.t1willi;

import java.util.Map;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.security.session.Session;

@Controller
public class AuthController extends BaseController {

    private final Map<String, String> users = Map.of("T1WiLLi", "admin1", "admin", "adminpass");

    @Post("/login")
    public JoltContext login(JoltContext context) {
        Form form = context.buildForm();
        String username = form.getValue("username");
        String password = form.getValue("password");

        if (username != null && password != null && users.containsKey(username)
                && users.get(username).equals(password)) {
            Session.setAuthenticated(true);
            Session.set("username", username);
            return context.redirect("/dashboard").status(HttpStatus.OK);
        } else {
            context.html(
                    "<!DOCTYPE html><html><body><h2>Login Failed</h2><p>Invalid username or password.</p><a href='/'>Try again</a></body></html>")
                    .status(HttpStatus.UNAUTHORIZED);
            return context;
        }
    }

    @Get("/dashboard")
    public JoltContext dashboard(JoltContext context) {
        if (!Session.isAuthenticated()) {
            return context.redirect("/");
        }

        String sessionId = Session.getSessionId();
        String ipAddress = Session.getIpAddress();
        String userAgent = Session.getUserAgent();
        String accessTime = Session.getAccess();
        String expireTime = Session.getExpire();
        String username = Session.get("username");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head><title>Dashboard</title></head>");
        sb.append("<body>");
        sb.append("<h2>Dashboard</h2>");
        sb.append("<h3>User Info</h3>");
        sb.append("<p>Username: ").append(username != null ? username : "N/A").append("</p>");
        sb.append("<h3>Session Info</h3>");
        sb.append("<p>Session ID: ").append(sessionId).append("</p>");
        sb.append("<p>IP Address: ").append(ipAddress).append("</p>");
        sb.append("<p>User Agent: ").append(userAgent).append("</p>");
        sb.append("<p>Access Time: ").append(accessTime).append("</p>");
        sb.append("<p>Expire Time: ").append(expireTime).append("</p>");
        sb.append("<p>Authenticated: ").append(Session.isAuthenticated()).append("</p>");
        sb.append("<a href='/logout'>Logout</a>");
        sb.append("</body>");
        sb.append("</html>");

        context.html(sb.toString());
        return context;
    }

    @Get("/logout")
    public JoltContext logout(JoltContext context) {
        Session.setAuthenticated(false);
        Session.invalidate();
        return context.redirect("/").status(HttpStatus.OK);
    }
}
