package ca.jolt.routing.builder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieBuilder {
    private final HttpServletResponse res;
    private String name;
    private String value;
    private boolean httpOnly;
    private boolean secure;
    private int maxAge = -1; // default: not set
    private String path;
    private String domain;
    private String sameSite; // e.g. "Strict", "Lax", "None"

    public CookieBuilder(HttpServletResponse res) {
        this.res = res;
    }

    public CookieBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public CookieBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public CookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public CookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public CookieBuilder maxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public CookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    public CookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public CookieBuilder sameSite(String sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    /**
     * Creates the Cookie using the configured values and adds it to the response.
     * Throws an exception if the name is not provided.
     */
    public void build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Cookie name may not be null or empty");
        }
        Cookie cookie = new Cookie(name, value != null ? value : "");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        if (path != null) {
            cookie.setPath(path);
        }
        if (domain != null) {
            cookie.setDomain(domain);
        }
        if (sameSite != null) {
            cookie.setAttribute("SameSite", sameSite);
        }
        res.addCookie(cookie);
    }
}