package ca.jolt.http;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A utility class that provides static factory methods for creating HTTP
 * requests
 * using the {@link RequestBuilder}.
 * 
 * <p>
 * This class simplifies the process of constructing HTTP requests by providing
 * shorthand methods for the most common HTTP methods: {@code GET},
 * {@code POST},
 * {@code PUT}, and {@code DELETE}.
 * 
 * <p>
 * Additionally, it provides access to a default {@link ObjectMapper} instance
 * for
 * handling JSON serialization and deserialization.
 * 
 * <p>
 * <strong>Example Usage:</strong>
 * 
 * <pre>{@code
 * Http.get("https://example.com")
 *         .header("Authorization", "Bearer token")
 *         .execute();
 * }</pre>
 * 
 * @author William Beaudin
 * @since 1.0
 */
public final class Http {

    /**
     * A default {@link ObjectMapper} instance used for JSON processing.
     */
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Http() {
        // Prevent instantiation
    }

    /**
     * Retrieves the default {@link ObjectMapper} instance.
     * 
     * @return the default {@code ObjectMapper} for JSON processing
     */
    public static ObjectMapper defaultMapper() {
        return DEFAULT_MAPPER;
    }

    /**
     * Creates a new {@link RequestBuilder} for an HTTP {@code GET} request.
     * 
     * @param url the URL to send the request to
     * @return a {@code RequestBuilder} preconfigured for a {@code GET} request
     */
    public static RequestBuilder get(String url) {
        return new RequestBuilder(HttpMethod.GET, url);
    }

    /**
     * Creates a new {@link RequestBuilder} for an HTTP {@code POST} request.
     * 
     * @param url the URL to send the request to
     * @return a {@code RequestBuilder} preconfigured for a {@code POST} request
     */
    public static RequestBuilder post(String url) {
        return new RequestBuilder(HttpMethod.POST, url);
    }

    /**
     * Creates a new {@link RequestBuilder} for an HTTP {@code PUT} request.
     * 
     * @param url the URL to send the request to
     * @return a {@code RequestBuilder} preconfigured for a {@code PUT} request
     */
    public static RequestBuilder put(String url) {
        return new RequestBuilder(HttpMethod.PUT, url);
    }

    /**
     * Creates a new {@link RequestBuilder} for an HTTP {@code DELETE} request.
     * 
     * @param url the URL to send the request to
     * @return a {@code RequestBuilder} preconfigured for a {@code DELETE} request
     */
    public static RequestBuilder delete(String url) {
        return new RequestBuilder(HttpMethod.DELETE, url);
    }
}