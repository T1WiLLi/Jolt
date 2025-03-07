package ca.jolt.http;

/**
 * Represents common HTTP status codes, each with a numeric code and descriptive
 * reason phrase. Also provides methods for categorizing status codes and
 * resolving a code to a corresponding {@link HttpStatus} enum constant.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * HttpStatus status = HttpStatus.OK;
 * System.out.println(status.code()); // 200
 * System.out.println(status.reason()); // "OK"
 * System.out.println(status.category()); // StatusCategory.SUCCESS
 * }</pre>
 *
 * <p>
 * To convert a numeric status code to an enum constant, you can use
 * {@link #fromCode(int)}:
 * 
 * <pre>{@code
 * HttpStatus status404 = HttpStatus.fromCode(404);
 * System.out.println(status404); // "404 Not Found"
 * }</pre>
 *
 * @author William Beaudin
 * @since 1.0
 */
public enum HttpStatus {

    /**
     * {@code 200 OK} - The request has succeeded.
     */
    OK(200, "OK"),

    /**
     * {@code 201 Created} - The request has been fulfilled and has resulted in a
     * new resource being created.
     */
    CREATED(201, "Created"),

    /**
     * {@code 204 No Content} - The server has successfully fulfilled the request
     * and there is no content to send in the response payload body.
     */
    NO_CONTENT(204, "No Content"),

    /**
     * {@code 301 Moved Permanently} - The requested resource has been assigned a
     * new permanent URI.
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),

    /**
     * {@code 302 Found} - The requested resource resides temporarily under a
     * different URI.
     */
    FOUND(302, "Found"),

    /**
     * {@code 303 See Other} - The response to the request can be found under
     * another URI
     * and should be retrieved using a GET method on that resource.
     */
    SEE_OTHER(303, "See Other"),

    /**
     * {@code 304 Not Modified} - Indicates that the resource has not been modified
     * since
     * the version specified by the request headers.
     */
    NOT_MODIFIED(304, "Not Modified"),

    /**
     * {@code 307 Temporary Redirect} - The requested resource resides temporarily
     * under a different URI.
     */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),

    /**
     * {@code 308 Permanent Redirect} - The target resource has been assigned a new
     * permanent URI.
     */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    /**
     * {@code 405 Method Not Allowed} - The method specified in the request-line is
     * not allowed for the resource.
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    /**
     * {@code 409 Conflict} - The request could not be completed due to a conflict
     * with the current state of the resource.
     */
    CONFLICT(409, "Conflict"),

    /**
     * {@code 400 Bad Request} - The server cannot or will not process the request
     * due to client error.
     */
    BAD_REQUEST(400, "Bad Request"),

    /**
     * {@code 401 Unauthorized} - The request requires user authentication.
     */
    UNAUTHORIZED(401, "Unauthorized"),

    /**
     * {@code 403 Forbidden} - The server understood the request, but refuses to
     * authorize it.
     */
    FORBIDDEN(403, "Forbidden"),

    /**
     * {@code 404 Not Found} - The requested resource could not be found on this
     * server.
     */
    NOT_FOUND(404, "Not Found"),

    /**
     * {@code 408 Request Timeout} - The server timed out waiting for the request.
     */
    REQUEST_TIMEOUT(408, "Request Timeout"),

    /**
     * {@code 413 Payload Too Large} - The request entity is larger than the server
     * is willing or able to process.
     */
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),

    /**
     * {@code 415 Unsupported Media Type} - The media format of the requested data
     * is not supported by the server.
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    /**
     * {@code 429 Too Many Requests} - The user has sent too many requests in a
     * given amount of time.
     */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    /**
     * {@code 500 Internal Server Error} - The server encountered an unexpected
     * condition that prevented it from fulfilling the request.
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

    /**
     * {@code 501 Not Implemented} - The server does not support the functionality
     * required to fulfill the request.
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),

    /**
     * {@code 503 Service Unavailable} - The server is currently unable to handle
     * the request due to temporary overloading or maintenance.
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    /**
     * A categorization of HTTP status codes, indicating the general type of
     * response.
     */
    public enum StatusCategory {
        /**
         * Codes in the range 100–199.
         */
        INFORMATIONAL,

        /**
         * Codes in the range 200–299.
         */
        SUCCESS,

        /**
         * Codes in the range 300–399.
         */
        REDIRECTION,

        /**
         * Codes in the range 400–499.
         */
        CLIENT_ERROR,

        /**
         * Codes in the range 500–599.
         */
        SERVER_ERROR,

        /**
         * Codes outside known HTTP status ranges.
         */
        UNKNOWN;
    }

    private final int code;
    private final String reason;

    /**
     * Retrieves a {@code HttpStatus} enum constant corresponding to the provided
     * numeric code.
     * If no matching status is found, {@link #NOT_IMPLEMENTED} is returned.
     *
     * @param code
     *             The numeric HTTP status code.
     * @return
     *         The corresponding {@code HttpStatus} enum constant, or
     *         {@code NOT_IMPLEMENTED} if no match exists.
     */
    public static HttpStatus fromCode(int code) {
        for (HttpStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        // Fallback if code is not recognized
        return NOT_IMPLEMENTED;
    }

    /**
     * Constructs a new {@code HttpStatus} enum constant.
     *
     * @param code
     *               The numeric HTTP status code.
     * @param reason
     *               A brief, descriptive phrase for this status (e.g., "OK", "Not
     *               Found").
     */
    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    /**
     * Returns the numeric code for this status (e.g., 200, 404, 500).
     *
     * @return
     *         The integer status code.
     */
    public int code() {
        return code;
    }

    /**
     * Returns the textual reason phrase (e.g., "OK", "Not Found").
     *
     * @return
     *         The reason phrase.
     */
    public String reason() {
        return reason;
    }

    /**
     * Determines the general category of this HTTP status code
     * (e.g., {@code SUCCESS} for 2xx codes, {@code CLIENT_ERROR} for 4xx codes).
     *
     * @return
     *         A {@link StatusCategory} reflecting the code range.
     */
    public StatusCategory category() {
        if (code >= 100 && code < 200) {
            return StatusCategory.INFORMATIONAL;
        }
        if (code >= 200 && code < 300) {
            return StatusCategory.SUCCESS;
        }
        if (code >= 300 && code < 400) {
            return StatusCategory.REDIRECTION;
        }
        if (code >= 400 && code < 500) {
            return StatusCategory.CLIENT_ERROR;
        }
        if (code >= 500 && code < 600) {
            return StatusCategory.SERVER_ERROR;
        }
        return StatusCategory.UNKNOWN;
    }

    /**
     * Returns a string representation of this status, combining the code and reason
     * phrase.
     * <p>
     * For instance, {@link #OK} returns "200 OK".
     *
     * @return
     *         A string in the format "{code} {reason}".
     */
    @Override
    public String toString() {
        return code + " " + reason;
    }
}