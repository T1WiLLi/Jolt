package io.github.t1willi.http.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.http.HttpStatus;

/**
 * A simple but powerful HTTP client interface with a fluent API.
 * 
 * <p>
 * This interface provides straightforward methods for making HTTP requests
 * and processing responses with minimal boilerplate code.
 * 
 * <p>
 * <strong>Usage example:</strong>
 * 
 * <pre>{@code
 * // Simple GET request
 * Pokedex pokedex = HttpClient.create()
 *         .sync(HttpMethod.GET, "https://pokeapi.co/api/v2/pokemon?limit=1000")
 *         .as(Pokedex.class);
 * 
 * // Async request
 * CompletableFuture<HttpRequest> future = HttpClient.create()
 *         .async(HttpMethod.GET, "https://pokeapi.co/api/v2/pokemon?limit=1000");
 * future.thenApply(request -> request.as(Pokedex.class))
 *         .thenAccept(pokedex -> processData(pokedex));
 * 
 * // POST request with headers and body
 * User user = HttpClient.create()
 *         .sync(HttpMethod.POST, "https://api.example.com/users")
 *         .withHeader("Content-Type", "application/json")
 *         .withHeader("Authorization", "Bearer token123")
 *         .withBody("{\"name\":\"John\",\"age\":30}")
 *         .timeout(Duration.ofSeconds(10))
 *         .as(User.class);
 * }</pre>
 * 
 * @author William Beaudin
 * @since 2.6.5
 */
public interface HttpClient extends AutoCloseable {

    /**
     * Creates a new HTTP client with default settings.
     * <p>
     * With default settings :
     * <ul>
     * <li>Timeout: 30 seconds</li>
     * <li>FollowRedirects: false</li>
     * </ul>
     * 
     * Those settings are based-off other instances of web development.
     * 
     * @return A new HTTP client instance
     */
    static HttpClient create() {
        return HttpClientFactory.create(Duration.ofSeconds(30), false);
    }

    /**
     * Creates a new HTTP client with the specified timeout and redirect settings.
     * 
     * @param timeout         The connection timeout
     * @param followRedirects Whether to follow redirects
     * @return A new HTTP client instance
     */
    static HttpClient create(Duration timeout, boolean followRedirects) {
        return HttpClientFactory.create(timeout, followRedirects);
    }

    /**
     * Initiate an asynchronous HTTP request.
     * 
     * @param method The HTTP method to use
     * @param uri    The URI to send the request to
     * @return A {@link CompletableFuture} containing the underlying HttpRequest for
     *         further processing
     */
    CompletableFuture<HttpRequest> async(HttpMethod method, String uri);

    /**
     * Initiate a synchronous HTTP request.
     * 
     * @param method The HTTP method to use
     * @param uri    The URI to send the request to
     * @return A {@link HttpRequest} for further configuration
     */
    HttpRequest sync(HttpMethod method, String uri);

    @Override
    void close();

    interface HttpRequest {
        /**
         * Adds a request header.
         * 
         * @param name  The header name
         * @param value The header value
         * @return This request for method chaining
         */
        HttpRequest withHeader(String name, String value);

        /**
         * Sets multiple request headers at once.
         * 
         * @param headers A map of header names to values
         * @return This request for method chaining
         */
        HttpRequest withHeaders(Map<String, String> headers);

        /**
         * Sets the request body as an object to be serialized to JSON.
         * 
         * @param body The object to serialize
         * @return This request for method chaining
         * @throws IOException If serialization fails
         */
        HttpRequest withBody(Object body) throws IOException;

        /**
         * Sets a timeout for this specific request.
         * 
         * @param timeout The request timeout
         * @return This request for method chaining
         */
        HttpRequest timeout(Duration timeout);

        /**
         * Returns the HTTP status code of the response.
         * 
         * @return The HTTP status code
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        int status() throws IOException, InterruptedException;

        /**
         * Returns the HTTP status object for the response.
         * 
         * @return The HTTP status object
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        HttpStatus httpStatus() throws IOException, InterruptedException;

        /**
         * Returns the response headers.
         * 
         * @return The response headers
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        HttpHeaders headers() throws IOException, InterruptedException;

        /**
         * Deserializes the response body to the specified class.
         * 
         * @param <T> The type to deserialize to
         * @param cls The class to deserialize to
         * @return The deserialized object
         * @throws IOException          If deserialization fails
         * @throws InterruptedException If the operation is interrupted
         */
        <T> T as(Class<T> cls) throws IOException, InterruptedException;

        /**
         * Deserializes the response body to the specified type reference.
         * 
         * @param <T>     The type to deserialize to
         * @param typeRef The type reference
         * @return The deserialized object
         * @throws IOException          If deserialization fails
         * @throws InterruptedException If the operation is interrupted
         */
        <T> T as(TypeReference<T> typeRef) throws IOException, InterruptedException;

        /**
         * Deserializes the response body to a list of the specified class.
         * 
         * @param <T> The type of elements in the list
         * @param cls The class of elements in the list
         * @return The deserialized list
         * @throws IOException          If deserialization fails
         * @throws InterruptedException If the operation is interrupted
         */
        <T> List<T> asList(Class<T> cls) throws IOException, InterruptedException;

        /**
         * Returns the response body as a string.
         * 
         * @return The response body as a string
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        String asString() throws IOException, InterruptedException;

        /**
         * Returns the response body as a byte array.
         * 
         * @return The response body as a byte array
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        byte[] asBytes() throws IOException, InterruptedException;

        /**
         * Writes the response body to the specified writer.
         * 
         * @param writer The writer to write to
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        void writeTo(Writer writer) throws IOException, InterruptedException;

        /**
         * Writes the response body to the specified output stream.
         * 
         * @param out The output stream to write to
         * @throws IOException          If an I/O error occurs
         * @throws InterruptedException If the operation is interrupted
         */
        void writeTo(OutputStream out) throws IOException, InterruptedException;
    }
}
