package ca.jolt.cookie;

import ca.jolt.injector.JoltContainer;
import lombok.Getter;

public abstract class CookieConfiguration {

    private static volatile CookieConfiguration INSTANCE;

    public static synchronized CookieConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = JoltContainer.getInstance().getBean(CookieConfiguration.class);

            INSTANCE.configure();
        }
        return INSTANCE;
    }

    @Getter
    private String sessionCookieName = "session";
    @Getter
    private String jwtCookieName = "jwt_token";

    @Getter
    private String sessionCookiePath = "/";
    @Getter
    private String jwtCookiePath = "/";

    @Getter
    private String sessionSameSitePolicy = "Strict";
    @Getter
    private String jwtSameSitePolicy = "Strict";

    public abstract CookieConfiguration configure();

    public CookieConfiguration sessionName(String name) {
        this.sessionCookieName = name;
        return this;
    }

    public CookieConfiguration jwtName(String name) {
        this.jwtCookieName = name;
        return this;
    }

    public CookieConfiguration sessionPath(String path) {
        this.sessionCookiePath = path;
        return this;
    }

    public CookieConfiguration jwtPath(String path) {
        this.jwtCookiePath = path;
        return this;
    }

    public CookieConfiguration sessionSameSite(String policy) {
        this.sessionSameSitePolicy = policy;
        return this;
    }

    public CookieConfiguration jwtSameSite(String policy) {
        this.jwtSameSitePolicy = policy;
        return this;
    }
}
