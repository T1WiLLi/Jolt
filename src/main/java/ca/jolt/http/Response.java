package ca.jolt.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.files.JoltFile;

/**
 * Represents an HTTP response received from a request made via
 * {@link RequestBuilder}.
 * Provides methods to access response status, headers, body as text, JSON, or
 * binary data.
 *
 * <p>
 * This class encapsulates an {@link HttpResponse} and an {@link ObjectMapper}
 * for JSON deserialization.
 *
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 * Response response = Http.get("https://api.example.com/data").execute();
 * if (response.isSuccessful()) {
 *     String content = response.text();
 *     System.out.println("Response: " + content);
 * }
 * }</pre>
 *
 * @author William Beaudin
 * @since 1.0
 */
public final class Response {
    private final HttpResponse<byte[]> httpResponse;
    private final ObjectMapper mapper;

    /**
     * Constructs a {@code Response} object wrapping an HTTP response.
     *
     * @param httpResponse The original HTTP response.
     * @param mapper       The JSON {@link ObjectMapper} used for deserialization.
     */
    public Response(HttpResponse<byte[]> httpResponse, ObjectMapper mapper) {
        this.httpResponse = httpResponse;
        this.mapper = mapper;
    }

    /**
     * Retrieves the HTTP status code of the response.
     *
     * @return The HTTP status code.
     */
    public int status() {
        return httpResponse.statusCode();
    }

    /**
     * Retrieves a specific header value from the response.
     *
     * @param name The header name.
     * @return The header value, or an empty string if not found.
     */
    public String header(String name) {
        return httpResponse.headers().firstValue(name).orElse("");
    }

    /**
     * Retrieves all response headers as a map.
     *
     * @return A map of header names to their first associated value.
     */
    public Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        httpResponse.headers().map().forEach((key, values) -> {
            if (!values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        });
        return headers;
    }

    /**
     * Retrieves the response body as a UTF-8 encoded string.
     *
     * @return The response body as a string.
     */
    public String text() {
        return new String(httpResponse.body(), StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the raw response body as a byte array.
     *
     * @return The response body in binary form.
     */
    public byte[] bytes() {
        return httpResponse.body();
    }

    /**
     * Parses the response body as a JSON object of the specified class type.
     *
     * @param <T>   The type of object to deserialize into.
     * @param clazz The class type to map the JSON response to.
     * @return The deserialized object.
     * @throws IOException If deserialization fails.
     */
    public <T> T json(Class<T> clazz) throws IOException {
        return mapper.readValue(httpResponse.body(), clazz);
    }

    /**
     * Parses the response body as a JSON object using a {@link TypeReference},
     * allowing for deserialization into generic types.
     *
     * @param <T>           The type of object to deserialize into.
     * @param typeReference The {@link TypeReference} specifying the target type.
     * @return The deserialized object.
     * @throws IOException If deserialization fails.
     */
    public <T> T json(TypeReference<T> typeReference) throws IOException {
        return mapper.readValue(httpResponse.body(), typeReference);
    }

    /**
     * Converts the response body into a {@link JoltFile}, using the content type
     * and
     * filename from the response headers when available.
     *
     * @return A {@link JoltFile} representing the response content.
     */
    public JoltFile file() {
        String filename = "download";
        String contentDisposition = header("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            filename = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9).replace("\"", "");
        }

        String contentType = header("Content-Type");
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return new JoltFile(filename, contentType, httpResponse.body().length, httpResponse.body());
    }

    /**
     * Determines whether the response indicates a successful request (status
     * 200-299).
     *
     * @return {@code true} if the response has a successful status code, otherwise
     *         {@code false}.
     */
    public boolean isSuccessful() {
        int code = status();
        return code >= 200 && code < 300;
    }
}
