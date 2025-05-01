package io.github.t1willi.security.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.github.t1willi.security.csrf.CsrfHandler;
import io.github.t1willi.security.csrf.DefaultCsrfHandler;
import io.github.t1willi.utils.Constant;

/**
 * Configuration class for CSRF protection settings.
 * <p>
 * This class allows enabling/disabling CSRF protection, configuring ignored URL
 * patterns,
 * setting the token name, and specifying a custom {@link CsrfHandler} for token
 * transmission.
 * </p>
 * <p>
 * Example usage:
 * 
 * <pre>
 *      .withCSRF()
 *         .enable()
 *         .addIgnoreUrlPatterns("/login", "/register")
 *         .withTokenName("_custom_csrf");
 * </pre>
 */
public class CsrfConfiguration {
    private boolean enabled = true;
    private boolean httpOnly = true;
    private String tokenName = Constant.Security.CSRF_TOKEN_NAME;
    private Set<String> ignoreUrlPatterns = new HashSet<>();
    private CsrfHandler handler = new DefaultCsrfHandler();
    private SecurityConfiguration securityConfig;

    public CsrfConfiguration(SecurityConfiguration securityConfig) {
        this.securityConfig = securityConfig;
    }

    public CsrfConfiguration enable() {
        this.enabled = true;
        return this;
    }

    public CsrfConfiguration disable() {
        this.enabled = false;
        return this;
    }

    public CsrfConfiguration addIgnoreUrlPatterns(String... patterns) {
        this.ignoreUrlPatterns.addAll(Arrays.asList(patterns));
        return this;
    }

    public CsrfConfiguration withHttpOnlyFalse() {
        this.httpOnly = false;
        return this;
    }

    public CsrfConfiguration withHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public CsrfConfiguration withTokenName(String tokenName) {
        this.tokenName = tokenName != null && !tokenName.isEmpty() ? tokenName : "_csrf";
        return this;
    }

    public CsrfConfiguration withHandler(CsrfHandler handler) {
        this.handler = handler != null ? handler : new DefaultCsrfHandler();
        return this;
    }

    public SecurityConfiguration and() {
        return securityConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public String getTokenName() {
        return tokenName;
    }

    public Set<String> getIgnoreUrlPatterns() {
        return Collections.unmodifiableSet(ignoreUrlPatterns);
    }

    public CsrfHandler getHandler() {
        return handler;
    }
}