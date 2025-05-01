package io.github.t1willi.http.api;

import java.net.http.HttpClient.Builder;
import java.time.Duration;

import io.github.t1willi.http.impl.DefaultHttpClient;
import io.github.t1willi.http.json.DefaultJsonSerializer;

public final class HttpClientFactory {
    private HttpClientFactory() {
        // No-Op
    }

    public static HttpClient create(Duration timeout, boolean followRedirects) {
        Builder builder = java.net.http.HttpClient.newBuilder()
                .connectTimeout(timeout);
        if (followRedirects) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.ALWAYS);
        }
        return new DefaultHttpClient(builder.build(), new DefaultJsonSerializer());
    }
}
