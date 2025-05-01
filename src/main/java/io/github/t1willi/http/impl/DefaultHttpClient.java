
package io.github.t1willi.http.impl;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import io.github.t1willi.http.api.HttpClient;
import io.github.t1willi.http.api.HttpResponse;
import io.github.t1willi.http.json.JsonSerializer;

public final class DefaultHttpClient implements HttpClient {
    private final java.net.http.HttpClient client;
    private final JsonSerializer json;

    public DefaultHttpClient(java.net.http.HttpClient client, JsonSerializer json) {
        this.client = client;
        this.json = json;
    }

    @Override
    public HttpResponse async(String uri) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(java.net.URI.create(uri))
                .build();
        var raw = client.send(req, BodyHandlers.ofByteArray());
        return new HttpResponse(raw, json);
    }

    @Override
    public void close() {
        // if you had a custom executor: shutdown it here
    }
}
