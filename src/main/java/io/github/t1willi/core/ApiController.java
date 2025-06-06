package io.github.t1willi.core;

import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.http.HttpStatus;

/**
 * An abstract base class for API controllers, providing utility methods to
 * construct HTTP response entities
 * with common status codes and content types.
 * <p>
 * This class extends {@link BaseController} and offers protected methods to
 * create standardized
 * {@link ResponseEntity} objects for typical HTTP responses, such as OK (200),
 * Created (201),
 * No Content (204), Bad Request (400), and Not Found (404). These methods are
 * designed to simplify
 * the process of building responses in RESTful API controllers by encapsulating
 * common response patterns,
 * such as setting JSON or plain text content types and handling response
 * headers. Subclasses can use
 * these methods to return consistent, well-formed HTTP responses in a web
 * application context.
 *
 * @since 1.0.0
 */
public abstract class ApiController extends BaseController {

    /**
     * Constructs a successful HTTP 200 OK response with a JSON body.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 200 (OK)
     * and the provided
     * body serialized as JSON. The response includes a "Content-Type" header set to
     * "application/json".
     * It is intended for use in RESTful API endpoints where the response contains
     * data, such as a
     * successful GET or POST request. The method is type-safe, allowing any object
     * type to be passed
     * as the body, which will be serialized by the underlying framework.
     *
     * @param <T>  the type of the response body
     * @param body the object to include in the response body, serialized as JSON
     * @return a {@link ResponseEntity} with HTTP status 200, JSON content type, and
     *         the provided body
     * @throws IllegalArgumentException if the body cannot be serialized to JSON
     * @since 1.0.0
     */
    protected <T> ResponseEntity<T> okJson(T body) {
        return ResponseEntity.ok(body)
                .header("Content-Type", "application/json");
    }

    /**
     * Constructs an HTTP 201 Created response with a specified location header.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 201
     * (Created) and no response
     * body. It sets the "Location" header to the provided URI, which typically
     * points to the newly created
     * resource. This is commonly used in RESTful APIs for POST requests that create
     * a new resource, where
     * the location header informs the client where to find the created resource.
     *
     * @param location the URI of the newly created resource, included in the
     *                 "Location" header
     * @return a {@link ResponseEntity} with HTTP status 201 and the specified
     *         location header
     * @throws IllegalArgumentException if the location is null or an invalid URI
     * @since 1.0.0
     */
    protected ResponseEntity<Void> created(String location) {
        return ResponseEntity.created(null, location);
    }

    /**
     * Constructs an HTTP 204 No Content response.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 204 (No
     * Content) and no
     * response body. It is typically used in RESTful APIs for operations that
     * successfully complete
     * without returning data, such as DELETE requests or updates that do not
     * require a response body.
     *
     * @return a {@link ResponseEntity} with HTTP status 204 and no content
     * @since 1.0.0
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent();
    }

    /**
     * Constructs an HTTP 400 Bad Request response with a body.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 400 (Bad
     * Request) and the provided body. The response includes a "Content-Type" header
     * set to "text/plain" if the body is a String, or "application/json" otherwise.
     * It is intended for use in RESTful APIs when the client sends an invalid
     * request,
     * such as malformed input or missing required parameters. The body can be a
     * String message or a structured object (e.g., for JSON serialization).
     *
     * @param <T>  the type of the response body
     * @param body the object to include in the response body
     * @return a {@link ResponseEntity} with HTTP status 400 and the appropriate
     *         content type
     * @throws IllegalArgumentException if the body is null
     * @since 1.0.1
     */
    protected <T> ResponseEntity<T> badRequest(T body) {
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
        String contentType = body instanceof String ? "text/plain" : "application/json";
        return ResponseEntity.of(HttpStatus.BAD_REQUEST, body)
                .header("Content-Type", contentType);
    }

    /**
     * Constructs an HTTP 404 Not Found response with a body.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 404 (Not
     * Found) and the provided body. The response includes a "Content-Type" header
     * set to "text/plain" if the body is a String, or "application/json" otherwise.
     * It is used in RESTful APIs when a requested resource cannot be found, such as
     * when a client attempts to access a non-existent entity. The body can be a
     * String message or a structured object (e.g., for JSON serialization).
     *
     * @param <T>  the type of the response body
     * @param body the object to include in the response body
     * @return a {@link ResponseEntity} with HTTP status 404 and the appropriate
     *         content type
     * @throws IllegalArgumentException if the body is null
     * @since 1.0.1
     */
    protected <T> ResponseEntity<T> notFound(T body) {
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
        String contentType = body instanceof String ? "text/plain" : "application/json";
        return ResponseEntity.of(HttpStatus.NOT_FOUND, body)
                .header("Content-Type", contentType);
    }
}