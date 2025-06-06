package io.github.t1willi.http;

import lombok.Getter;

import java.util.*;

import io.github.t1willi.template.JoltModel;

/**
 * A wrapper class for constructing and managing HTTP responses, encapsulating
 * status, headers, body, and redirect flag.
 * <p>
 * The {@code ResponseEntity} class provides a type-safe, immutable
 * representation of an HTTP response, including the
 * HTTP status code, headers, body, and a flag indicating whether the response
 * is a redirect. It is designed for use in
 * web applications, particularly in RESTful APIs and MVC frameworks, to build
 * standardized HTTP responses. The class
 * exposes its functionality through static factory methods for creating
 * instances and instance methods for modifying
 * response attributes in a fluent, chainable manner. The generic type parameter
 * allows flexibility in the type of the
 * response body.
 *
 * @param <T> the type of the response body
 * @since 1.0.0
 */
@Getter
public class ResponseEntity<T> {
    private final HttpStatus status;
    private final Map<String, List<String>> headers;
    private final T body;
    private final boolean redirect;

    /**
     * Constructs a new ResponseEntity with the specified attributes.
     *
     * @param status   the HTTP status code
     * @param headers  the response headers, as a map of header names to lists of
     *                 values
     * @param body     the response body
     * @param redirect whether this response represents a redirect
     */
    private ResponseEntity(HttpStatus status,
            Map<String, List<String>> headers,
            T body,
            boolean redirect) {
        this.status = status;
        this.headers = headers != null ? headers : new LinkedHashMap<>();
        this.body = body;
        this.redirect = redirect;
    }

    /**
     * Creates a ResponseEntity with the specified status, body, redirect flag, and
     * headers.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with all four
     * components: HTTP status,
     * response body, redirect flag, and headers. It is the most comprehensive
     * factory method for creating
     * a response entity, allowing full customization of all attributes. The headers
     * map is used as-is, and
     * the redirect flag indicates whether the response is a redirect (e.g., HTTP
     * 302).
     *
     * @param <U>      the type of the response body
     * @param status   the HTTP status code for the response
     * @param body     the response body, or null if none
     * @param redirect whether this response is a redirect
     * @param headers  the response headers, as a map of header names to lists of
     *                 values
     * @return a new {@code ResponseEntity} instance
     * @throws IllegalArgumentException if the status is null
     * @since 1.0.0
     */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body,
            boolean redirect,
            Map<String, List<String>> headers) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return new ResponseEntity<>(status, headers, body, redirect);
    }

    /**
     * Creates a ResponseEntity with the specified status and body, with no redirect
     * and empty headers.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with the given
     * HTTP status and body,
     * setting the redirect flag to false and using an empty headers map. It is
     * useful for simple responses
     * where only the status and body are needed, such as a 200 OK response with
     * data.
     *
     * @param <U>    the type of the response body
     * @param status the HTTP status code for the response
     * @param body   the response body, or null if none
     * @return a new {@code ResponseEntity} instance
     * @throws IllegalArgumentException if the status is null
     * @since 1.0.0
     */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return new ResponseEntity<>(status, new LinkedHashMap<>(), body, false);
    }

    /**
     * Creates a ResponseEntity with the specified status, with no body, no
     * redirect, and empty headers.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with the given
     * HTTP status, no body,
     * a false redirect flag, and an empty headers map. It is useful for responses
     * that require only a
     * status code, such as 204 No Content or 404 Not Found without a body.
     *
     * @param status the HTTP status code for the response
     * @return a new {@code ResponseEntity} instance with no body
     * @throws IllegalArgumentException if the status is null
     * @since 1.0.0
     */
    public static ResponseEntity<Void> of(
            HttpStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return new ResponseEntity<>(status, new LinkedHashMap<>(), null, false);
    }

    /**
     * Creates a ResponseEntity with the specified status, body, and redirect flag,
     * with empty headers.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with the given
     * HTTP status, body,
     * and redirect flag, using an empty headers map. It is useful for responses
     * that need a body and
     * redirect indication but no custom headers, such as a redirect with additional
     * data.
     *
     * @param <U>      the type of the response body
     * @param status   the HTTP status code for the response
     * @param body     the response body, or null if none
     * @param redirect whether this response is a redirect
     * @return a new {@code ResponseEntity} instance
     * @throws IllegalArgumentException if the status is null
     * @since 1.0.1
     */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body,
            boolean redirect) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return new ResponseEntity<>(status, new LinkedHashMap<>(), body, redirect);
    }

    /**
     * Creates a ResponseEntity with the specified status, body, and single-value
     * headers.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with the given
     * HTTP status, body,
     * and a map of single-value headers, converting each header to a single-element
     * list internally.
     * The redirect flag is set to false. This is useful for responses where headers
     * are simple key-value
     * pairs, such as setting a single "Content-Type" header.
     *
     * @param <U>                the type of the response body
     * @param status             the HTTP status code for the response
     * @param body               the response body, or null if none
     * @param singleValueHeaders a map of header names to single header values
     * @return a new {@code ResponseEntity} instance
     * @throws IllegalArgumentException if the status is null or if the headers map
     *                                  contains null keys or values
     * @since 1.0.0
     */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body,
            Map<String, String> singleValueHeaders) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (singleValueHeaders == null || singleValueHeaders.containsKey(null)
                || singleValueHeaders.containsValue(null)) {
            throw new IllegalArgumentException("Headers map cannot contain null keys or values");
        }
        var multi = new LinkedHashMap<String, List<String>>();
        singleValueHeaders.forEach((k, v) -> multi.put(k, List.of(v)));
        return new ResponseEntity<>(status, multi, body, false);
    }

    /**
     * Creates a 200 OK ResponseEntity with the specified body.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 200 OK,
     * the provided body, no redirect, and empty headers. It is commonly used for
     * successful responses
     * that return data, such as GET or POST requests in a RESTful API.
     *
     * @param <U>  the type of the response body
     * @param body the response body, or null if none
     * @return a new {@code ResponseEntity} instance with HTTP status 200 OK
     * @since 1.0.0
     */
    public static <U> ResponseEntity<U> ok(U body) {
        return of(HttpStatus.OK, body);
    }

    /**
     * Creates a 204 No Content ResponseEntity with no body.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 204 No
     * Content, no body, no redirect, and empty headers. It is typically used for
     * successful operations
     * that do not return data, such as DELETE or PATCH requests.
     *
     * @return a new {@code ResponseEntity} instance with HTTP status 204 No Content
     * @since 1.0.0
     */
    public static ResponseEntity<Void> noContent() {
        return of(HttpStatus.NO_CONTENT);
    }

    /**
     * Creates a 201 Created ResponseEntity with the specified body and Location
     * header.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 201 Created,
     * the provided body, and a "Location" header set to the specified URI. The
     * redirect flag is false.
     * It is commonly used for POST requests that create a new resource, where the
     * Location header
     * indicates the URI of the created resource.
     *
     * @param <U>      the type of the response body
     * @param body     the response body, or null if none
     * @param location the URI for the "Location" header
     * @return a new {@code ResponseEntity} instance with HTTP status 201 Created
     * @throws IllegalArgumentException if the location is null or empty
     * @since 1.0.0
     */
    public static <U> ResponseEntity<U> created(U body, String location) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Location", List.of(location));
        return of(HttpStatus.CREATED, body, false, headers);
    }

    /**
     * Creates a 201 Created ResponseEntity with the specified body.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 201 Created,
     * It is commonly used for POST requests that creates a new resource, where the
     * body is the new resource.
     * 
     * @param <U>  The type of the response body
     * @param body the response body, or null if none
     * @return a new {@code ResponseEntity} instance with HTTP status 201 Created
     */
    public <U> ResponseEntity<U> created(U body) {
        return of(HttpStatus.CONFLICT, body);
    }

    /**
     * Creates a 404 Not Found ResponseEntity with an error message header.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 404 Not
     * Found, no body, and an "X-Error-Message" header containing the specified
     * message. The redirect
     * flag is false. It is used when a requested resource cannot be found.
     *
     * @param message the error message to include in the "X-Error-Message" header
     * @return a new {@code ResponseEntity} instance with HTTP status 404 Not Found
     * @throws IllegalArgumentException if the message is null or empty
     * @since 1.0.0
     */
    public static ResponseEntity<Void> notFound(String message) {
        return of(HttpStatus.NOT_FOUND, null, Map.of("X-Error-Message", message));
    }

    /**
     * Creates a 302 Found ResponseEntity for a redirect with a Location header and
     * no body.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 302 Found,
     * no body, a redirect flag set to true, and a "Location" header set to the
     * specified URI. It is
     * used for redirecting clients to another URL, typically after a POST request.
     *
     * @param location the URI for the "Location" header
     * @return a new {@code ResponseEntity} instance with HTTP status 302 Found
     * @throws IllegalArgumentException if the location is null or empty
     * @since 1.0.1
     */
    public static ResponseEntity<Void> redirect(String location) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Location", List.of(location));
        return of(HttpStatus.FOUND, null, true, headers);
    }

    /**
     * Creates a 302 Found ResponseEntity for a redirect with a Location header and
     * a JoltModel body.
     * <p>
     * This static factory method constructs a {@code ResponseEntity} with an HTTP
     * status of 302 Found,
     * a {@link JoltModel} body, a redirect flag set to true, and a "Location"
     * header set to the specified
     * URI. The model is carried to the target endpoint, typically for use in a
     * template if the target
     * is a view, or ignored otherwise. This is useful in MVC frameworks for
     * POST-redirect-GET patterns
     * with model data.
     *
     * @param location the URI for the "Location" header
     * @param model    the {@link JoltModel} to carry to the target endpoint
     * @return a new {@code ResponseEntity} instance with HTTP status 302 Found
     * @throws IllegalArgumentException if the location is null or empty
     * @since 1.0.1
     */
    public static ResponseEntity<JoltModel> redirect(
            String location,
            JoltModel model) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Location", List.of(location));
        return new ResponseEntity<>(HttpStatus.FOUND, headers, model, true);
    }

    /**
     * Sets the Content-Type header of the response, returning a modified instance.
     * <p>
     * This method sets the "Content-Type" header to the specified value, replacing
     * any existing values for the header. It returns the same
     * {@code ResponseEntity}
     * instance for method chaining, modifying the headers map in place. This is a
     * convenience method for setting the Content-Type header commonly used in HTTP
     * responses.
     *
     * @param contentType the value for the "Content-Type" header
     * @return this {@code ResponseEntity} instance for method chaining
     * @throws IllegalArgumentException if the contentType is null or empty
     * @since 1.0.1
     */
    public ResponseEntity<T> contentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            throw new IllegalArgumentException("Content-Type cannot be null or empty");
        }
        headers.put("Content-Type", List.of(contentType));
        return this;
    }

    /**
     * Adds a header to the response entity, returning a modified instance.
     * <p>
     * This method adds the specified header name and value to the response's
     * headers map, appending the
     * value to the list of values for that header if it already exists. It returns
     * the same
     * {@code ResponseEntity} instance for method chaining, modifying the headers
     * map in place.
     *
     * @param name  the name of the header to add
     * @param value the value of the header
     * @return this {@code ResponseEntity} instance for method chaining
     * @throws IllegalArgumentException if the header name or value is null
     * @since 1.0.0
     */
    public ResponseEntity<T> header(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("Header name or value cannot be null");
        }
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Creates a new ResponseEntity with the specified status, preserving other
     * attributes.
     * <p>
     * This method constructs a new {@code ResponseEntity} with the provided HTTP
     * status, copying the
     * existing headers, body, and redirect flag from this instance. It is useful
     * for modifying the
     * status code of an existing response while keeping other attributes unchanged.
     *
     * @param newStatus the new HTTP status code
     * @return a new {@code ResponseEntity} instance with the updated status
     * @throws IllegalArgumentException if the new status is null
     * @since 1.0.0
     */
    public ResponseEntity<T> status(HttpStatus newStatus) {
        return new ResponseEntity<>(
                newStatus,
                new LinkedHashMap<>(this.headers),
                this.body,
                this.redirect);
    }

    /**
     * Creates a new ResponseEntity with the specified body, preserving other
     * attributes.
     * <p>
     * This method constructs a new {@code ResponseEntity} with the provided body,
     * copying the existing
     * status, headers, and redirect flag from this instance. It allows changing the
     * response body type
     * and content while keeping other attributes unchanged.
     *
     * @param <U>     the type of the new response body
     * @param newBody the new response body, or null if none
     * @return a new {@code ResponseEntity} instance with the updated body
     * @since 1.0.0
     */
    public <U> ResponseEntity<U> body(U newBody) {
        return new ResponseEntity<>(
                this.status,
                new LinkedHashMap<>(this.headers),
                newBody,
                this.redirect);
    }
}