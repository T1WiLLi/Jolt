package ca.jolt.cookie;

import ca.jolt.injector.JoltContainer;
import lombok.Getter;

/**
 * Provides configuration for cookies, including names, paths, and policies.
 * <p>
 * This class can be extended to customize cookie-related settings, such as
 * the session cookie name or path. Instances are typically obtained through
 * the static {@link #getInstance()} method, which ensures a single global
 * instance. This instance is retrieved from the dependency injector.
 */
public abstract class CookieConfiguration {

    /**
     * Holds the singleton instance of this configuration.
     */
    private static volatile CookieConfiguration INSTANCE;

    /**
     * Returns the global instance of {@code CookieConfiguration}.
     * <p>
     * If no instance exists, this method retrieves it from {@link JoltContainer}.
     *
     * @return The singleton configuration instance
     */
    public static synchronized CookieConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = JoltContainer.getInstance().getBean(CookieConfiguration.class);
        }
        return INSTANCE;
    }

    /**
     * The name of the session cookie.
     */
    @Getter
    private String sessionCookieName = "JSESSIONID";

    /**
     * The name of the JWT cookie.
     */
    @Getter
    private String jwtCookieName = "JWT-TOKEN";

    /**
     * The path under which the session cookie is valid.
     */
    @Getter
    private String sessionCookiePath = "/";

    /**
     * The path under which the JWT cookie is valid.
     */
    @Getter
    private String jwtCookiePath = "/";

    /**
     * The SameSite policy for session cookies.
     */
    @Getter
    private String sessionSameSitePolicy = "Strict";

    /**
     * The SameSite policy for JWT cookies.
     */
    @Getter
    private String jwtSameSitePolicy = "Strict";

    /**
     * Performs any necessary configuration steps for this cookie configuration.
     *
     * @return This configuration instance for chaining
     */
    public abstract CookieConfiguration configure();

    /**
     * Sets the name of the session cookie.
     *
     * @param name The new session cookie name
     * @return This configuration instance for chaining
     */
    public CookieConfiguration sessionName(String name) {
        this.sessionCookieName = name;
        return this;
    }

    /**
     * Sets the name of the JWT cookie.
     *
     * @param name The new JWT cookie name
     * @return This configuration instance for chaining
     */
    public CookieConfiguration jwtName(String name) {
        this.jwtCookieName = name;
        return this;
    }

    /**
     * Sets the path under which the session cookie is valid.
     *
     * @param path The desired path for the session cookie
     * @return This configuration instance for chaining
     */
    public CookieConfiguration sessionPath(String path) {
        this.sessionCookiePath = path;
        return this;
    }

    /**
     * Sets the path under which the JWT cookie is valid.
     *
     * @param path The desired path for the JWT cookie
     * @return This configuration instance for chaining
     */
    public CookieConfiguration jwtPath(String path) {
        this.jwtCookiePath = path;
        return this;
    }

    /**
     * Sets the SameSite policy for session cookies.
     *
     * @param policy The desired SameSite policy for session cookies
     * @return This configuration instance for chaining
     */
    public CookieConfiguration sessionSameSite(String policy) {
        this.sessionSameSitePolicy = policy;
        return this;
    }

    /**
     * Sets the SameSite policy for JWT cookies.
     *
     * @param policy The desired SameSite policy for JWT cookies
     * @return This configuration instance for chaining
     */
    public CookieConfiguration jwtSameSite(String policy) {
        this.jwtSameSitePolicy = policy;
        return this;
    }
}
