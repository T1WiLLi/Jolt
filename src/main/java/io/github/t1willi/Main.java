package io.github.t1willi;

import java.util.Map;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.security.utils.JwtToken;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    public void setup() {
        String jwe = JwtToken.jwe("1", Map.of("username", "t1willi"));
        String jws = JwtToken.jws("1", Map.of("username", "t1willi"));

        get("/", ctx -> ctx.html("JWE: " + jwe + "<br>JWS: " + jws));
    }
}
