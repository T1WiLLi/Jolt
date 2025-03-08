package ca.jolt.cookie;

import ca.jolt.injector.JoltContainer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds and adds {@link Cookie} objects to an {@link HttpServletResponse}.
 * <p>
 * This class follows the builder pattern, allowing a series of configuration
 * calls before invoking {@link #build()} to finalize the cookie and attach it
 * to the response.
 * <p>
 * <strong>Example Usage:</strong>
 * 
 * <pre>{@code
 * HttpServletResponse response = ...;
 * new CookieBuilder(response)
 *     .setName("sessionId")
 *     .setValue("abc123")
 *     .httpOnly(true)
 *     .secure(true)
 *     .maxAge(3600)
 *     .path("/")
 *     .sameSite("Strict")
 *     .build();
 * }</pre>
 * <p>
 * If the cookie name is absent or empty, {@link IllegalStateException} is
 * thrown
 * when {@link #build()} is called.
 *
 * @author William
 * @since 1.0
 */
public final class CookieBuilder {

    /**
     * The {@link HttpServletResponse} to which the resulting cookie will be added.
     */
    private final HttpServletResponse res;

    /**
     * The name of the cookie.
     */
    private String name;

    /**
     * The value of the cookie.
     */
    private String value;

    /**
     * Indicates whether the cookie is HTTP-only.
     */
    private boolean httpOnly;

    /**
     * Indicates whether the cookie is secure.
     */
    private boolean secure;

    /**
     * Specifies the maximum age of the cookie in seconds.
     */
    private int maxAge = -1;

    /**
     * Specifies the path for which the cookie is valid.
     */
    private String path;

    /**
     * Specifies the domain for which the cookie is valid.
     */
    private String domain;

    /**
     * Specifies the {@code SameSite} policy for the cookie.
     */
    private String sameSite;

    /**
     * The configuration for the cookie.
     */
    private static CookieConfiguration cookieConfig;

    /**
     * Creates a new cookie builder for the given HTTP response.
     *
     * @param res The HTTP response to which the cookie will be added
     */
    public CookieBuilder(HttpServletResponse res) {
        this.res = res;
        try {
            cookieConfig = JoltContainer.getInstance().getBean(CookieConfiguration.class);
        } catch (Exception e) {
            // Ignore and use default configuration
        }
    }

    /**
     * Sets the cookie name.
     *
     * @param name The name of the cookie
     * @return This builder for chaining
     */
    public CookieBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the cookie value.
     *
     * @param value The value of the cookie
     * @return This builder for chaining
     */
    public CookieBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    /**
     * Sets whether this cookie is HTTP-only.
     *
     * @param httpOnly {@code true} if the cookie is HTTP-only; {@code false}
     *                 otherwise
     * @return This builder for chaining
     */
    public CookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    /**
     * Sets whether this cookie is secure.
     *
     * @param secure {@code true} if the cookie is secure; {@code false} otherwise
     * @return This builder for chaining
     */
    public CookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Sets the maximum age of the cookie in seconds.
     *
     * @param maxAge The maximum age in seconds
     * @return This builder for chaining
     */
    public CookieBuilder maxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * Sets the path for which the cookie is valid.
     *
     * @param path The path on which the cookie is valid
     * @return This builder for chaining
     */
    public CookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Sets the domain for which the cookie is valid.
     *
     * @param domain The domain on which the cookie is valid
     * @return This builder for chaining
     */
    public CookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Sets the {@code SameSite} policy for the cookie.
     *
     * @param sameSite The desired SameSite policy (for example, "Strict")
     * @return This builder for chaining
     */
    public CookieBuilder sameSite(String sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    /**
     * Creates and adds the cookie to the HTTP response.
     *
     * @throws IllegalStateException if the cookie name is null or empty
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

    /**
     * Creates a session cookie with secure and HTTP-only flags enabled.
     *
     * @param value The cookie value to store
     */
    public void sessionCookie(String value) {
        setName(cookieConfig.getSessionCookieName());
        setValue(value);
        secure(true);
        httpOnly(true);
        path(cookieConfig.getSessionCookiePath());
        sameSite(cookieConfig.getSessionSameSitePolicy());
        build();
    }

    /**
     * Creates a JWT cookie with secure and HTTP-only flags enabled.
     *
     * @param value  The JWT string value
     * @param maxAge The maximum age of the cookie in seconds
     */
    public void jwtCookie(String value, int maxAge) {
        setName(cookieConfig.getJwtCookieName());
        setValue(value);
        secure(true);
        httpOnly(true);
        path(cookieConfig.getJwtCookiePath());
        sameSite(cookieConfig.getJwtSameSitePolicy());
        maxAge(maxAge);
        build();
    }

    /**
     * Creates an unsecure cookie with minimal security settings.
     * <p>
     * <strong>Warning!</strong> This cookie is accessible via JavaScript.
     *
     * @param name  The cookie name
     * @param value The cookie value
     */
    public void unsecureCookie(String name, String value) {
        setName(name);
        setValue(value);
        secure(false);
        httpOnly(false);
        path("/");
        sameSite("Lax");
        build();
    }

    /*-----------------------------------
     * COOKIE CONFIGURATION VALUES ACCESS
     * ----------------------------------
     */

    public static String sessionCookieName() {
        return cookieConfig.getSessionCookieName();
    }

    public static String jwtCookieName() {
        return cookieConfig.getJwtCookieName();
    }

    public static String sessionCookiePath() {
        return cookieConfig.getSessionCookiePath();
    }

    public static String jwtCookiePath() {
        return cookieConfig.getJwtCookiePath();
    }

    public static String sessionCookieDomain() {
        return cookieConfig.getSessionSameSitePolicy();
    }

    public static String jwtSameSitePolicy() {
        return cookieConfig.getJwtSameSitePolicy();
    }
}