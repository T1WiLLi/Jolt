
package io.github.t1willi.http.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.http.api.HttpClient;
import io.github.t1willi.http.json.JsonSerializer;

public final class DefaultHttpClient implements HttpClient {
    private final java.net.http.HttpClient client;
    private final JsonSerializer json;

    public DefaultHttpClient(java.net.http.HttpClient client, JsonSerializer json) {
        this.client = client;
        this.json = json;
    }

    @Override
    public CompletableFuture<HttpRequest> async(HttpMethod method, String uri) {
        DefaultHttpRequest request = new DefaultHttpRequest(method, uri);
        return CompletableFuture.completedFuture(request);
    }

    @Override
    public HttpRequest sync(HttpMethod method, String uri) {
        return new DefaultHttpRequest(method, uri);
    }

    @Override
    public void close() {
        // Close any resources if needed
    }

    /**
     * Default implementation of the HttpRequest interface.
     */
    private class DefaultHttpRequest implements HttpRequest {
        private final HttpMethod method;
        private final String uri;
        private final Map<String, String> headers = new HashMap<>();
        private BodyPublisher bodyPublisher = BodyPublishers.noBody();
        private Duration timeout = null;

        /**
         * Creates a new DefaultHttpRequest.
         * 
         * @param method The HTTP method
         * @param uri    The URI
         */
        DefaultHttpRequest(HttpMethod method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        @Override
        public HttpRequest withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        @Override
        public HttpRequest withHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        @Override
        public HttpRequest withBody(Object body) throws IOException {
            byte[] jsonBytes = json.toJson(body);
            this.bodyPublisher = BodyPublishers.ofByteArray(jsonBytes);
            if (!headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "application/json");
            }
            return this;
        }

        @Override
        public HttpRequest timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        private java.net.http.HttpResponse<byte[]> sendRequest() throws IOException, InterruptedException {
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .method(method.name(), bodyPublisher);

            headers.forEach(requestBuilder::header);
            if (timeout != null) {
                requestBuilder.timeout(timeout);
            }
            return client.send(requestBuilder.build(), BodyHandlers.ofByteArray());
        }

        @Override
        public int status() throws IOException, InterruptedException {
            return sendRequest().statusCode();
        }

        @Override
        public HttpStatus httpStatus() throws IOException, InterruptedException {
            return HttpStatus.fromCode(status());
        }

        @Override
        public java.net.http.HttpHeaders headers() throws IOException, InterruptedException {
            return sendRequest().headers();
        }

        @Override
        public <T> T as(Class<T> cls) throws IOException, InterruptedException {
            var response = sendRequest();
            return json.fromJson(response.body(), cls);
        }

        @Override
        public <T> T as(TypeReference<T> typeRef) throws IOException, InterruptedException {
            var response = sendRequest();
            return json.fromJson(response.body(), typeRef);
        }

        @Override
        public <T> List<T> asList(Class<T> cls) throws IOException, InterruptedException {
            var response = sendRequest();
            return json.fromJson(
                    response.body(),
                    new TypeReference<List<T>>() {
                        @Override
                        public java.lang.reflect.Type getType() {
                            return json.getMapper()
                                    .getTypeFactory()
                                    .constructCollectionType(List.class, cls);
                        }
                    });
        }

        @Override
        public String asString() throws IOException, InterruptedException {
            var response = sendRequest();
            return new String(response.body(), StandardCharsets.UTF_8);
        }

        @Override
        public byte[] asBytes() throws IOException, InterruptedException {
            var response = sendRequest();
            return response.body();
        }

        @Override
        public void writeTo(Writer writer) throws IOException, InterruptedException {
            String body = asString();
            writer.write(body);
        }

        @Override
        public void writeTo(OutputStream out) throws IOException, InterruptedException {
            byte[] bytes = asBytes();
            out.write(bytes);
        }
    }
}
