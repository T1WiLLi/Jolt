package io.github.t1willi.http;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration representing the standard HTTP methods used in web requests.
 * 
 * <p>
 * This enum provides constants for the most commonly used HTTP methods:
 * {@code GET}, {@code POST}, {@code PUT}, {@code DELETE}, {@code PATCH},
 * {@code HEAD}, {@code OPTIONS}, and {@code TRACE}.
 * 
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 * HttpMethod method = HttpMethod.GET;
 * }</pre>
 * 
 * @author William Beaudin
 * @since 1.0
 */
public enum HttpMethod {

    /**
     * The HTTP {@code GET} method, used to retrieve data from a server.
     */
    GET,

    /**
     * The HTTP {@code POST} method, used to send data to a server to create or
     * update a resource.
     */
    POST,

    /**
     * The HTTP {@code PUT} method, used to update an existing resource on a server.
     */
    PUT,

    /**
     * The HTTP {@code DELETE} method, used to remove a resource from a server.
     */
    DELETE,

    /**
     * The HTTP {@code PATCH} method, used to apply partial modifications to a
     * resource.
     */
    PATCH,

    /**
     * The HTTP {@code HEAD} method, used to retrieve metadata about a resource.
     */
    HEAD,

    /**
     * The HTTP {@code OPTIONS} method, used to describe the communication options
     * for the target resource.
     */
    OPTIONS,

    /**
     * The HTTP {@code TRACE} method, used to perform a message loop-back test along
     * the path to the target resource.
     */
    TRACE;

    private static final Map<String, HttpMethod> STRING_TO_ENUM = new HashMap<>();

    static {
        for (HttpMethod method : values()) {
            STRING_TO_ENUM.put(method.name(), method);
        }
    }

    /**
     * Converts a string to its corresponding {@code HttpMethod} enum value.
     * 
     * @param method the string representation of the HTTP method
     * @return the corresponding {@code HttpMethod} enum value
     * @throws IllegalArgumentException if the string does not match any HTTP method
     */
    public static HttpMethod fromString(String method) {
        HttpMethod httpMethod = STRING_TO_ENUM.get(method.toUpperCase());
        if (httpMethod == null) {
            throw new IllegalArgumentException("Unknown HTTP method: " + method);
        }
        return httpMethod;
    }

    /**
     * Converts the {@code HttpMethod} enum value to its string representation.
     * 
     * @return the string representation of the HTTP method
     */
    @Override
    public String toString() {
        return name();
    }
}
