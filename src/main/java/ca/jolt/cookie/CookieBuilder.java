package ca.jolt.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A builder class for constructing and adding {@link Cookie} objects to an
 * {@link HttpServletResponse}. This utility follows the builder pattern,
 * allowing a series of configuration calls prior to invoking {@link #build()}
 * to finalize the cookie and attach it to the response.
 *
 * <p>
 * <strong>Example Usage:</strong>
 * </p>
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
 *     .build(); // Adds the constructed Cookie to the response
 * }</pre>
 *
 * <p>
 * After the cookie is created and configured, calling {@link #build()}
 * creates the actual {@code Cookie} object and attaches it to the response.
 * If the cookie name is absent or empty, an {@link IllegalStateException}
 * is thrown.
 * </p>
 *
 * @author William Beaudin
 * @since 1.0
 */
public final class CookieBuilder {

    /**
     * The {@link HttpServletResponse} to which the resulting cookie will be added.
     */
    private final HttpServletResponse res;

    private String name;
    private String value;
    private boolean httpOnly;
    private boolean secure;
    private int maxAge = -1; // default: not set
    private String path;
    private String domain;
    private String sameSite; // e.g., "Strict", "Lax", or "None"

    /**
     * Creates a new {@code CookieBuilder} that will attach cookies to the specified
     * {@link HttpServletResponse}.
     *
     * @param res
     *            The response object to which the cookie will be added.
     */
    public CookieBuilder(HttpServletResponse res) {
        this.res = res;
    }

    /**
     * Sets the name of the cookie.
     * <p>
     * This field is mandatory. If it is not set or is empty,
     * {@link #build()} will throw an exception.
     * </p>
     *
     * @param name
     *             The cookie name.
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the value of the cookie. Defaults to an empty string if unset.
     *
     * @param value
     *              The cookie value.
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    /**
     * Enables or disables the {@code HttpOnly} attribute of the cookie.
     * <p>
     * When set to {@code true}, client-side scripts cannot access the cookie,
     * enhancing security against certain cross-site scripting (XSS) attacks.
     * </p>
     *
     * @param httpOnly
     *                 {@code true} to set the cookie as HTTP only; {@code false}
     *                 otherwise.
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    /**
     * Enables or disables the {@code Secure} attribute of the cookie.
     * <p>
     * When set to {@code true}, the browser will only send the cookie
     * over secure (HTTPS) connections.
     * </p>
     *
     * @param secure
     *               {@code true} to require a secure connection; {@code false}
     *               otherwise.
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Sets the maximum age of the cookie in seconds.
     * <p>
     * A negative value means the cookie is not stored persistently and
     * will be deleted when the Web browser exits. A zero value causes
     * the cookie to be deleted immediately.
     * </p>
     *
     * @param maxAge
     *               The maximum age in seconds.
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder maxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * Sets the path for the cookie.
     * <p>
     * This indicates a URL path for which the cookie is valid, such as {@code "/"}
     * for the entire site or a specific path like {@code "/app"}.
     * If unset, defaults to the request path.
     * </p>
     *
     * @param path
     *             The path under which the cookie is valid.
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Sets the domain for the cookie.
     * <p>
     * If not specified, the browser typically uses the host name of the page
     * that sets the cookie.
     * </p>
     *
     * @param domain
     *               The domain for which the cookie is valid (e.g.,
     *               {@code "example.com"}).
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Sets the {@code SameSite} attribute for the cookie, controlling cross-site
     * behavior. Common values are {@code "Strict"}, {@code "Lax"}, or
     * {@code "None"}.
     * <p>
     * {@code "Strict"} prevents the browser from sending this cookie along
     * with cross-site requests entirely,
     * {@code "Lax"} allows sending the cookie with same-site and some cross-site
     * GET requests, and
     * {@code "None"} allows sending the cookie in all contexts (but requires
     * {@link #secure(boolean) secure} to be {@code true}).
     * </p>
     *
     * @param sameSite
     *                 The desired SameSite policy (e.g., "Strict", "Lax", or
     *                 "None").
     * @return
     *         This {@code CookieBuilder} for chaining.
     */
    public CookieBuilder sameSite(String sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    /**
     * Creates a new {@link Cookie} based on the configuration in this builder
     * and adds it to the {@link HttpServletResponse}.
     * <p>
     * If the name has not been set or is empty, an
     * {@link IllegalStateException} is thrown.
     * </p>
     *
     * <p>
     * Usage example:
     * </p>
     * 
     * <pre>{@code
     * CookieBuilder builder = new CookieBuilder(response)
     *         .setName("myCookie")
     *         .setValue("someValue")
     *         .httpOnly(true)
     *         .secure(true);
     *
     * builder.build(); // Cookie is created and attached to response
     * }</pre>
     *
     * @throws IllegalStateException
     *                               If no cookie name is provided.
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
     * Creates a session cookie with secure and HttpOnly flags enabled.
     * <p>
     * Session cookies are temporary and will be deleted when the browser is closed.
     * These cookies are not stored persistently and have no expiration time.
     * They are suitable for maintaining session state during user interaction.
     * </p>
     * 
     * <p>
     * The cookie will be configured with:
     * </p>
     * <ul>
     * <li>name: "session"</li>
     * <li>secure: true</li>
     * <li>httpOnly: true</li>
     * <li>sameSite: "Strict"</li>
     * <li>path: "/"</li>
     * </ul>
     * 
     * @param value The cookie value to store
     */
    public void sessionCookie(String value) {
        setName(CookieConfiguration.getInstance().getSessionCookieName());
        setValue(value);
        secure(true);
        httpOnly(true);
        path(CookieConfiguration.getInstance().getSessionCookiePath());
        sameSite(CookieConfiguration.getInstance().getSessionSameSitePolicy());
        build();
    }

    /**
     * Creates a JWT cookie with appropriate security settings for storing
     * JSON Web Tokens.
     * <p>
     * JWT cookies are typically used for authentication and should be protected
     * with secure and HttpOnly flags. This method configures the cookie with
     * a specified expiration time in seconds.
     * </p>
     * 
     * <p>
     * The cookie will be configured with:
     * </p>
     * <ul>
     * <li>name: "jwt"</li>
     * <li>secure: true</li>
     * <li>httpOnly: true</li>
     * <li>sameSite: "Strict"</li>
     * <li>path: "/"</li>
     * <li>maxAge: as specified</li>
     * </ul>
     * 
     * @param value  The JWT string value
     * @param maxAge The maximum age of the cookie in seconds
     */
    public void jwtCookie(String value, int maxAge) {
        setName(CookieConfiguration.getInstance().getJwtCookieName());
        setValue(value);
        secure(true);
        httpOnly(true);
        path(CookieConfiguration.getInstance().getJwtCookiePath());
        sameSite(CookieConfiguration.getInstance().getJwtSameSitePolicy());
        maxAge(maxAge);
        build();
    }

    /**
     * Creates an unsecure cookie with minimal security settings.
     * <p>
     * <strong>WARNING:</strong> These cookies are accessible to JavaScript and
     * are transmitted over both HTTP and HTTPS connections. They should NOT be
     * used for sensitive information.
     * </p>
     * 
     * <p>
     * The cookie will be configured with:
     * </p>
     * <ul>
     * <li>secure: false</li>
     * <li>httpOnly: false</li>
     * <li>sameSite: "Lax"</li>
     * <li>path: "/"</li>
     * </ul>
     * 
     * @param name  The name of the cookie
     * @param value The value to store
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
}