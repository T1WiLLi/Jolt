package ca.jolt.http;

/**
 * An enumeration representing the standard HTTP methods used in web requests.
 * 
 * <p>
 * This enum provides constants for the four most commonly used HTTP methods:
 * {@code GET}, {@code POST}, {@code PUT}, and {@code DELETE}.
 * 
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
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
    DELETE;
}
