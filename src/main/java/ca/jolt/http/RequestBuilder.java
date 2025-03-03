package ca.jolt.http;

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

import ca.jolt.exceptions.FailedToBuildBodyException;
import ca.jolt.files.JoltFile;

public final class RequestBuilder {
    private final HttpMethod method;
    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private final List<QueryParam> queryParams = new ArrayList<>();

    private Object body;
    private Duration timeout = Duration.ofSeconds(10);
    private boolean secure = false; // if true, use https:// instead of http://
    private ObjectMapper mapper = Http.defaultMapper();

    public RequestBuilder(HttpMethod method, String url) {
        this.method = method;
        this.url = url;
    }

    public RequestBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public RequestBuilder headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public RequestBuilder query(String name, String value) {
        queryParams.add(new QueryParam(name, value));
        return this;
    }

    public RequestBuilder body(Object body) {
        this.body = body;
        header("Content-Type", "application/json");
        return this;
    }

    public RequestBuilder form(Map<String, String> formData) {
        this.body = formData;
        header("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }

    public RequestBuilder file(JoltFile file) {
        this.body = file;
        if (file.getContentType() != null) {
            header("Content-Type", file.getContentType());
        }
        return this;
    }

    public RequestBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public RequestBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public RequestBuilder mapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    public Response execute() throws IOException, InterruptedException {
        HttpClient client = createClient();
        HttpRequest request = createRequest();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return new Response(response, mapper);
    }

    public CompletableFuture<Response> executeAsync() {
        HttpClient client = createClient();
        HttpRequest request = createRequest();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> new Response(response, mapper));
    }

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

            if (body instanceof JoltFile) {
                JoltFile file = (JoltFile) body;
                return publisherFunction.apply(
                        requestBuilder,
                        HttpRequest.BodyPublishers.ofByteArray(file.getData())).build();
            }

            if (headers.getOrDefault("Content-Type", "").equals("application/json")) {
                byte[] jsonBytes = mapper.writeValueAsBytes(body);
                return publisherFunction.apply(
                        requestBuilder,
                        HttpRequest.BodyPublishers.ofByteArray(jsonBytes)).build();
            }

            if (headers.getOrDefault("Content-Type", "").equals("application/x-www-form-urlencoded")) {
                @SuppressWarnings("unchecked")
                Map<String, String> formData = (Map<String, String>) body;
                StringBuilder formBody = new StringBuilder();
                formData.forEach((key, value) -> {
                    if (formBody.length() > 0) {
                        formBody.append("&");
                    }
                    formBody.append(key).append("=").append(value); // URL encoding should be added
                });
                return publisherFunction.apply(
                        requestBuilder,
                        HttpRequest.BodyPublishers.ofString(formBody.toString())).build();
            }

            return publisherFunction.apply(
                    requestBuilder,
                    HttpRequest.BodyPublishers.ofString(body.toString())).build();
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
