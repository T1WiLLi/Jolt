package io.github.t1willi.security.policies;

import io.github.t1willi.security.config.HeadersConfiguration;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configures the Content Security Policy (CSP) directives with automatic nonce
 * injection if nonce support is enabled in the SecurityConfiguration.
 */
public class ContentSecurityPolicy {

    public static final String SELF = "'self'";
    public static final String NONE = "'none'";
    public static final String UNSAFE_INLINE = "'unsafe-inline'";
    public static final String UNSAFE_EVAL = "'unsafe-eval'";
    public static final String BLOB = "'blob:'";
    public static final String DATA = "'data:'";
    public static final String NONCE_PLACEHOLDER = "'nonce-{{NONCE}}'"; // Placeholder for runtime nonce injection

    public static final String GOOGLE_FONTS = "https://fonts.googleapis.com https://fonts.gstatic.com";
    public static final String CDNJS = "https://cdnjs.cloudflare.com";
    public static final String CLOUDFLARE = "https://*.cloudflare.com";

    @Getter
    private List<String> defaultSources = new ArrayList<>(List.of(SELF));
    @Getter
    private List<String> fontSources = new ArrayList<>();
    @Getter
    private List<String> scriptSources = new ArrayList<>();
    @Getter
    private List<String> scriptElementSources = new ArrayList<>();
    @Getter
    private List<String> scriptAttributeSources = new ArrayList<>();
    @Getter
    private List<String> styleSources = new ArrayList<>();
    @Getter
    private List<String> styleElementSources = new ArrayList<>();
    @Getter
    private List<String> styleAttributeSources = new ArrayList<>();
    @Getter
    private List<String> childSources = new ArrayList<>();
    @Getter
    private List<String> workerSources = new ArrayList<>();
    @Getter
    private List<String> connectSources = new ArrayList<>();
    @Getter
    private List<String> imageSources = new ArrayList<>();
    @Getter
    private List<String> frameSources = new ArrayList<>();
    @Getter
    private List<String> mediaSources = new ArrayList<>();

    private final HeadersConfiguration parent;

    public ContentSecurityPolicy(HeadersConfiguration parent) {
        this.parent = parent;
    }

    public ContentSecurityPolicy withDefaultSources(String... sources) {
        this.defaultSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withFontSources(String... sources) {
        this.fontSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withStyleSources(String... sources) {
        this.styleSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withStyleElementSources(String... sources) {
        this.styleElementSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withStyleAttributeSources(String... sources) {
        this.styleAttributeSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withScriptSources(String... sources) {
        this.scriptSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withScriptElementSources(String... sources) {
        this.scriptElementSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withScriptAttributeSources(String... sources) {
        this.scriptAttributeSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withChildSources(String... sources) {
        this.childSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withWorkerSources(String... sources) {
        this.workerSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withConnectSources(String... sources) {
        this.connectSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withImageSources(String... sources) {
        this.imageSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withFrameSources(String... sources) {
        this.frameSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public ContentSecurityPolicy withMediaSources(String... sources) {
        this.mediaSources = new ArrayList<>(Arrays.asList(sources));
        return this;
    }

    public HeadersConfiguration and() {
        return this.parent;
    }

    public String build() {
        StringBuilder csp = new StringBuilder("default-src ").append(String.join(" ", defaultSources));
        boolean nonceEnabled = isNonceEnabled();

        if (nonceEnabled) {
            if (!scriptSources.contains(NONCE_PLACEHOLDER)) {
                scriptSources.add(NONCE_PLACEHOLDER);
            }
            if (!scriptElementSources.contains(NONCE_PLACEHOLDER)) {
                scriptElementSources.add(NONCE_PLACEHOLDER);
            }
            if (!styleSources.contains(NONCE_PLACEHOLDER)) {
                styleSources.add(NONCE_PLACEHOLDER);
            }
            if (!styleElementSources.contains(NONCE_PLACEHOLDER)) {
                styleElementSources.add(NONCE_PLACEHOLDER);
            }
        }

        appendDirective(csp, "font-src", fontSources);
        appendDirective(csp, "style-src", styleSources);
        appendDirective(csp, "style-src-elem", styleElementSources);
        appendDirective(csp, "style-src-attr", styleAttributeSources);
        appendDirective(csp, "script-src", scriptSources);
        appendDirective(csp, "script-src-elem", scriptElementSources);
        appendDirective(csp, "script-src-attr", scriptAttributeSources);
        appendDirective(csp, "child-src", childSources);
        appendDirective(csp, "worker-src", workerSources);
        appendDirective(csp, "connect-src", connectSources);
        appendDirective(csp, "img-src", imageSources);
        appendDirective(csp, "frame-src", frameSources);
        appendDirective(csp, "media-src", mediaSources);

        String cspString = csp.append(";").toString();
        return cspString;
    }

    private void appendDirective(StringBuilder csp, String directiveName, List<String> sources) {
        if (!sources.isEmpty()) {
            csp.append("; ").append(directiveName).append(" ").append(String.join(" ", sources));
        }
    }

    private boolean isNonceEnabled() {
        return this.parent.and().getNonceConfig().isEnabled();
    }
}