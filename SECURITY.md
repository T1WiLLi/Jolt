# Security Policy

## Supported Versions

The following versions of Jolt are currently being supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 2.6.5 >   | :white_check_mark: |

We strongly recommend using the latest version to ensure you have all security patches and improvements. Please note that Jolt is currently in early development stages and is not yet recommended for production environments.

## Reporting a Vulnerability

I takes security vulnerabilities seriously. As Jolt is in an early stage of development and not yet recommended for production use, we currently handle security reports through our public GitHub repository.

### How to Report

To report a security vulnerability:

1.  Create a new issue on the [Jolt GitHub repository](https://github.com/T1WiLLi/Jolt/issues)
2.  Use the title format: "[SECURITY] Brief description of the issue"
3.  Include detailed information about the vulnerability, including:
    -   A description of the vulnerability
    -   Steps to reproduce
    -   Potential impact
    -   Suggested mitigation or fix (if any)
4.  If possible, include a minimal code sample or proof-of-concept that demonstrates the vulnerability

### What to Expect

-   **Initial Response**: We will acknowledge your report within 1-3 days.
-   **Collaboration**: We welcome your input on potential fixes and may ask for additional information.
-   **Resolution**: Since Jolt is in active development, security fixes will be prioritized based on severity and incorporated into upcoming releases.

### Disclosure Policy

-   We follow responsible disclosure principles.
-   We will work with you to understand and resolve the issue before any public disclosure.
-   We will credit you for your discovery (unless you prefer to remain anonymous).
-   Once the vulnerability is fixed, we will publish a security advisory detailing the issue and its resolution.

## Security Best Practices

When using Jolt, we recommend following these security best practices:

1.  **Keep Jolt Updated**: Always use the latest version of the framework. Note that Jolt is in early stages of development and should not be used for production applications yet.
2.  **Enable Security Features**: Use the built-in security features such as CSRF protection, authentication, and authorization.
3.  **Validate User Input**: Always validate and sanitize user input to prevent injection attacks.
4.  **Secure Your Dependencies**: Regularly audit and update your project dependencies.
5.  **Follow the Principle of Least Privilege**: Grant minimum necessary permissions to your application components.

## Security-Related Configuration

Jolt provides several security features that can be configured in your application:

```java

    withCORS()
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
        .allowedCredentials(false)
        .maxAge(3600);

    withHeaders()
        .withXssProtection(XssProtectionPolicy.ENABLE_BLOCK)
        .withFrameOptions(FrameOptionsPolicy.DENY)
        .withHsts(HstsPolicy.ONE_YEAR_WITH_SUBDOMAINS_PRELOAD)
        .withReferrerPolicy(ReferrerPolicy.SAME_ORIGIN)
        .httpsOnly(false)
        .withCacheControl(CacheControlPolicy.NO_CACHE);
        .withCSP() // Without preventing to simply pass a 'string' withCSP (string)
            .withFontSources("self", "https://example.com")
            .withStyleSources("self", "https://example.com", ContentSecurityPolicy.UNSAFE_INLINE)
            .withScriptSources("self", "https://example.com")
            .withChildSources("self")
            .withWorkerSources("blob:")
            .withConnectSources("self", "https://api.mapbox.com")
            .withImageSources("self", "blob", "data");

    .withCSRF()
        .enable() // .disable();
        .addIgnoreUrlPatterns("/login", "/register", "/forgot-password", "/reset-password");
        .withHttpOnlyFalse(); // .withHttpOnly(true);

    .withNonce()
        .enable();

    .withRoutes()
        .route("/public/**")
            .permitAll()
        .route("/threads/**")
            .methods(GET, PATCH)
            .authenticated()
        .route("/admin/**")
            .authenticated()
        .route("/users/**")
            .methods(GET)
            .authenticated()
        .route("/jokes")
            .methods(GET)
            .authenticated(new CustomAuthStrategy())
        .anyRoute()
            .denyAll();
```

## Security Updates

Security advisories for Jolt are published through:

1.  GitHub Security Advisories
2.  The official Jolt newsletter
3.  The security page on our website: https://jolt-framework.org/security

To stay informed about security updates, we recommend watching the GitHub repository and subscribing to our newsletter.
