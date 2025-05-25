package io.github.t1willi.http.api;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.http.json.JsonSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A wrapper class for {@link java.net.http.HttpResponse} with a byte array
 * body, providing convenient JSON deserialization.
 * <p>
 * The {@code HttpResponse} class encapsulates a raw
 * {@link java.net.http.HttpResponse} with a byte array body and
 * provides methods to access the response's status code, headers, and body, as
 * well as deserialize the body into
 * Java objects or lists using a {@link JsonSerializer}. It is designed for use
 * in web applications or APIs where
 * HTTP responses need to be processed and their JSON content deserialized into
 * type-safe objects. The class supports
 * both simple class-based deserialization and complex type deserialization
 * using Jackson's {@link TypeReference}.
 *
 * @since 1.0.0
 */
public class HttpResponse {
    private final java.net.http.HttpResponse<byte[]> raw;
    private final JsonSerializer json;

    /**
     * Constructs a new HttpResponse wrapping the provided raw HTTP response and
     * JSON serializer.
     *
     * @param raw  the raw {@link java.net.http.HttpResponse} with a byte array body
     * @param json the {@link JsonSerializer} used for JSON deserialization
     */
    public HttpResponse(java.net.http.HttpResponse<byte[]> raw, JsonSerializer json) {
        this.raw = raw;
        this.json = json;
    }

    /**
     * Retrieves the HTTP status code of the response.
     * <p>
     * This method returns the status code (e.g., 200, 404) of the underlying HTTP
     * response. It is useful for
     * determining the outcome of the HTTP request, such as success, client error,
     * or server error.
     *
     * @return the HTTP status code
     * @since 1.0.0
     */
    public int statusCode() {
        return raw.statusCode();
    }

    /**
     * Retrieves the HTTP headers of the response.
     * <p>
     * This method returns the {@link HttpHeaders} object containing all headers
     * from the underlying HTTP response.
     * The headers can be used to inspect metadata such as content type, cache
     * control, or custom headers.
     *
     * @return the {@link HttpHeaders} object containing the response headers
     * @since 1.0.0
     */
    public HttpHeaders headers() {
        return raw.headers();
    }

    /**
     * Retrieves the response body as a UTF-8 encoded string.
     * <p>
     * This method converts the raw byte array body of the HTTP response to a string
     * using UTF-8 encoding.
     * It is useful for accessing the raw response content, such as when the body is
     * plain text or JSON
     * that needs to be processed manually.
     *
     * @return the response body as a UTF-8 encoded string
     * @since 1.0.0
     */
    public String body() {
        return new String(raw.body(), StandardCharsets.UTF_8);
    }

    /**
     * Deserializes the response body into an object of the specified class.
     * <p>
     * This method uses the provided {@link JsonSerializer} to deserialize the
     * response body (assumed to be JSON)
     * into an instance of the specified class. It is useful for converting JSON
     * responses into type-safe
     * Java objects, such as POJOs representing API data.
     *
     * @param <T> the type of the object to deserialize into
     * @param cls the {@link Class} of the target object
     * @return the deserialized object of type {@code T}
     * @throws IOException              if an error occurs during JSON
     *                                  deserialization
     * @throws IllegalArgumentException if the class is null
     * @since 1.0.0
     */
    public <T> T as(Class<T> cls) throws IOException {
        return json.fromJson(raw.body(), cls);
    }

    /**
     * Deserializes the response body into a list of objects of the specified class.
     * <p>
     * This method uses the provided {@link JsonSerializer} to deserialize the
     * response body (assumed to be a JSON
     * array) into a {@link List} of objects of the specified class. It constructs a
     * {@link TypeReference} internally
     * to handle the list type. This is useful for processing JSON arrays returned
     * by APIs into type-safe lists
     * of Java objects.
     *
     * @param <T> the type of the objects in the list
     * @param cls the {@link Class} of the objects in the list
     * @return a {@link List} of deserialized objects of type {@code T}
     * @throws IOException              if an error occurs during JSON
     *                                  deserialization
     * @throws IllegalArgumentException if the class is null
     * @since 1.0.0
     */
    public <T> List<T> asList(Class<T> cls) throws IOException {
        return json.fromJson(
                raw.body(),
                new TypeReference<List<T>>() {
                    @Override
                    public Type getType() {
                        return json.getMapper()
                                .getTypeFactory()
                                .constructCollectionType(List.class, cls);
                    }
                });
    }

    /**
     * Deserializes the response body into an object of the specified type
     * reference.
     * <p>
     * This method uses the provided {@link JsonSerializer} to deserialize the
     * response body (assumed to be JSON)
     * into an object of the type specified by the {@link TypeReference}. This is
     * useful for handling complex
     * or generic types, such as nested objects or collections, that cannot be
     * represented by a simple class.
     *
     * @param <T>  the type of the object to deserialize into
     * @param type the {@link TypeReference} specifying the target type
     * @return the deserialized object of type {@code T}
     * @throws IOException              if an error occurs during JSON
     *                                  deserialization
     * @throws IllegalArgumentException if the type reference is null
     * @since 1.0.0
     */
    public <T> T as(TypeReference<T> type) throws IOException {
        return json.fromJson(raw.body(), type);
    }
}