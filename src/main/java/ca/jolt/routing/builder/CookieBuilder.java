package ca.jolt.routing.builder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieBuilder {
    private final HttpServletResponse res;
    private final Cookie cookie;

    public CookieBuilder(HttpServletResponse res) {
        this.res = res;
        this.cookie = new Cookie("", "");
    }

    public CookieBuilder from(String name, String value) {
        cookie.setAttribute("name", name);
        cookie.setValue(value);
        return this;
    }

    public CookieBuilder httpOnly(boolean httpOnly) {
        cookie.setHttpOnly(httpOnly);
        return this;
    }

    public CookieBuilder secure(boolean secure) {
        cookie.setSecure(secure);
        return this;
    }

    public CookieBuilder maxAge(int maxAge) {
        cookie.setMaxAge(maxAge);
        return this;
    }

    public CookieBuilder path(String path) {
        cookie.setPath(path);
        return this;
    }

    public CookieBuilder domain(String domain) {
        cookie.setDomain(domain);
        return this;
    }

    public CookieBuilder sameSite(String sameSite) {
        cookie.setAttribute("SameSite", sameSite);
        return this;
    }

    public void build() {
        res.addCookie(cookie);
    }
}
