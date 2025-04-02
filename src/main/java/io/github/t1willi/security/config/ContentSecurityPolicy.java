package io.github.t1willi.security.config;

import lombok.Getter;

public class ContentSecurityPolicy {
    public static final String SELF = "'self'";
    public static final String NONE = "'none'";
    public static final String UNSAFE_INLINE = "'unsafe-inline'";
    public static final String UNSAFE_EVAL = "'unsafe-eval'";
    public static final String BLOB = "'blob:'";
    public static final String DATA = "'data:'";

    @Getter
    private String defaultSrc = SELF;
    @Getter
    private String fontSrc;
    @Getter
    private String styleSrc;
    @Getter
    private String scriptSrc;
    @Getter
    private String childSrc;
    @Getter
    private String workerSrc;
    @Getter
    private String connectSrc;
    @Getter
    private String imageSrc;

    /**
     * Sets the default-src directive.
     */
    public ContentSecurityPolicy withDefaultSources(String... sources) {
        this.defaultSrc = String.join(" ", sources);
        return this;
    }

    /**
     * Sets the font-src directive.
     */
    public ContentSecurityPolicy withFontSources(String... sources) {
        this.fontSrc = "font-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Sets the style-src directive.
     */
    public ContentSecurityPolicy withStyleSources(String... sources) {
        this.styleSrc = "style-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Sets the script-src directive.
     */
    public ContentSecurityPolicy withScriptSources(String... sources) {
        this.scriptSrc = "script-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Sets the child-src directive.
     */
    public ContentSecurityPolicy withChildSources(String... sources) {
        this.childSrc = "child-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Sets the worker-src directive.
     */
    public ContentSecurityPolicy withWorkerSources(String... sources) {
        this.workerSrc = "worker-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Sets the connect-src directive.
     */
    public ContentSecurityPolicy withConnectSources(String... sources) {
        this.connectSrc = "connect-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Sets the img-src directive.
     */
    public ContentSecurityPolicy withImageSources(String... sources) {
        this.imageSrc = "img-src " + String.join(" ", sources);
        return this;
    }

    /**
     * Builds the CSP string from the configured directives.
     */
    public String build() {
        StringBuilder csp = new StringBuilder("default-src ").append(defaultSrc);
        appendDirective(csp, fontSrc);
        appendDirective(csp, styleSrc);
        appendDirective(csp, scriptSrc);
        appendDirective(csp, childSrc);
        appendDirective(csp, workerSrc);
        appendDirective(csp, connectSrc);
        appendDirective(csp, imageSrc);
        return csp.append(";").toString();
    }

    private void appendDirective(StringBuilder csp, String directive) {
        if (directive != null && !directive.isEmpty()) {
            csp.append("; ").append(directive);
        }
    }
}
