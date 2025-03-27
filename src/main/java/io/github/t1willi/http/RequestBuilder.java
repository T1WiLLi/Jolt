package io.github.t1willi.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.exceptions.FailedToBuildBodyException;
import io.github.t1willi.files.JoltFile;

/**
 * A builder class for constructing and executing HTTP requests using Java's
 * {@link HttpClient}. Supports synchronous and asynchronous execution,
 * JSON body serialization, query parameters, and file uploads.
 * 
 * <p>
 * The {@code RequestBuilder} allows for fluently building requests by
 * specifying HTTP methods, headers, body content, and other settings.
 *
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 * RequestBuilder request = Http.get("https://api.example.com/data")
 *         .header("Authorization", "Bearer token")
 *         .query("id", "123")
 *         .timeout(Duration.ofSeconds(5));
 * 
 * Response response = request.execute();
 * }</pre>
 * 
 * @author William Beaudin
 * @since 1.0
 */
public final class RequestBuilder {
    private final HttpMethod method;
    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private final List<QueryParam> queryParams = new ArrayList<>();

    private Object body;
    private Duration timeout = Duration.ofSeconds(10);
    private boolean secure = false; // if true, use https:// instead of http://
    private ObjectMapper mapper = Http.defaultMapper();

    /**
     * Initializes a new request builder for the specified HTTP method and URL.
     *
     * @param method The HTTP method (GET, POST, etc.).
     * @param url    The request URL.
     */
    public RequestBuilder(HttpMethod method, String url) {
        this.method = method;
        this.url = url;
    }

    /**
     * Adds a single header to the request.
     *
     * @param key   The header name.
     * @param value The header value.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Adds multiple headers to the request.
     *
     * @param headers A map containing headers and their values.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Adds a query parameter to the request URL.
     *
     * @param name  The parameter name.
     * @param value The parameter value.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder query(String name, String value) {
        queryParams.add(new QueryParam(name, value));
        return this;
    }

    /**
     * Sets the request body with a JSON-serializable object.
     *
     * @param body The object to be sent as JSON.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder body(Object body) {
        this.body = body;
        header("Content-Type", "application/json");
        return this;
    }

    /**
     * Sets the request body as a form-urlencoded payload.
     *
     * @param formData A map of form data.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder form(Map<String, String> formData) {
        this.body = formData;
        header("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }

    /**
     * Sets the request body as a file upload.
     *
     * @param file The file to be uploaded.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder file(JoltFile file) {
        this.body = file;
        if (file.getContentType() != null) {
            header("Content-Type", file.getContentType());
        }
        return this;
    }

    /**
     * Sets the timeout duration for the request.
     *
     * @param timeout The timeout duration.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Enables or disables HTTPS for the request.
     *
     * @param secure {@code true} for HTTPS, {@code false} for HTTP.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Sets a custom JSON {@link ObjectMapper} for serializing the request body.
     *
     * @param mapper The object mapper.
     * @return This {@code RequestBuilder} instance.
     */
    public RequestBuilder mapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    /**
     * Executes the HTTP request synchronously and returns the response.
     *
     * @return The {@link Response} containing the server's reply.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response execute() throws IOException, InterruptedException {
        HttpClient client = createClient();
        HttpRequest request = createRequest();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return new Response(response, mapper);
    }

    /**
     * Executes the HTTP request asynchronously.
     *
     * @return A {@link CompletableFuture} resolving to a {@link Response}.
     */
    public CompletableFuture<Response> executeAsync() {
        HttpClient client = createClient();
        HttpRequest request = createRequest();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> new Response(response, mapper));
    }

    /**
     * Executes the HTTP request asynchronously and passes the response to a
     * callback.
     *
     * @param callback A consumer that processes the response.
     */
    public void executeAsync(Consumer<Response> callback) {
        executeAsync().thenAccept(callback);
    }

    private HttpClient createClient() {
        return HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    private HttpRequest createRequest() {
        String protocol = secure ? "https://" : "http://";
        if (url.startsWith("http://") || url.startsWith("https://")) {
            protocol = "";
        }

        String fullUrl = protocol + url;
        if (!queryParams.isEmpty()) {
            StringBuilder urlWithQuery = new StringBuilder(fullUrl);
            urlWithQuery.append(fullUrl.contains("?") ? "&" : "?");

            for (int i = 0; i < queryParams.size(); i++) {
                QueryParam param = queryParams.get(i);
                if (i > 0) {
                    urlWithQuery.append("&");
                }
                urlWithQuery.append(encodeParam(param.name))
                        .append("=")
                        .append(encodeParam(param.value));
            }

            fullUrl = urlWithQuery.toString();
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(timeout);

        headers.forEach(requestBuilder::header);

        return switch (method) {
            case GET -> requestBuilder.GET().build();
            case POST -> buildBodyRequest(requestBuilder, HttpRequest.Builder::POST);
            case PUT -> buildBodyRequest(requestBuilder, HttpRequest.Builder::PUT);
            case DELETE -> requestBuilder.DELETE().build();
            default -> throw new UnsupportedOperationException("Unsupported HTTP method: " + method);
        };
    }

    private HttpRequest buildBodyRequest(
            HttpRequest.Builder requestBuilder,
            BodyPublisherFunction publisherFunction) {
        try {
            if (body == null) {
                return publisherFunction.apply(requestBuilder, HttpRequest.BodyPublishers.noBody()).build();
            }

            if (body instanceof JoltFile file) {
                return publisherFunction.apply(
                        requestBuilder,
                        HttpRequest.BodyPublishers.ofByteArray(file.getData())).build();
            }

            if ("application/json".equals(headers.get("Content-Type"))) {
                byte[] jsonBytes = mapper.writeValueAsBytes(body);
                return publisherFunction.apply(
                        requestBuilder,
                        HttpRequest.BodyPublishers.ofByteArray(jsonBytes)).build();
            }

            return publisherFunction.apply(requestBuilder, HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
        } catch (Exception e) {
            throw new FailedToBuildBodyException("Failed to build request body", e);
        }
    }

    private String encodeParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static record QueryParam(String name, String value) {
    }
}
