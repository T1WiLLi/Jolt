package io.github.t1willi;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.security.session.Session;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/session", ctx -> {
            Map<String, Object> dump = new LinkedHashMap<>();
            dump.put("sessionId", Session.getSessionId());
            dump.put("initialized", Session.get(Session.KEY_INITIALIZED, "false"));
            dump.put("ipAddress", Session.get(Session.KEY_IP_ADDRESS, null));
            dump.put("userAgent", Session.get(Session.KEY_USER_AGENT, null));
            dump.put("accessTime", Session.get(Session.KEY_ACCESS_TIME, null));
            dump.put("expireTime", Session.get(Session.KEY_EXPIRE_TIME, null));
            dump.put("authenticated", Session.isAuthenticated());
            dump.put("username", Session.get("username"));
            return ctx.json(dump);
        });

        get("/login", ctx -> {
            Session.set("username", "t1willi");
            Session.setAuthenticated(true);
            return ctx.redirect("/session");
        });

        get("/logout", ctx -> {
            Session.destroy();
            return ctx.redirect("/session");
        });
    }
}
