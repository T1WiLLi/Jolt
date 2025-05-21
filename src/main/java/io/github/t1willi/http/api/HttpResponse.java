// io.github.t1willi.jolt.http.api/HttpResponse.java
package io.github.t1willi.http.api;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.http.json.JsonSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wrapper around java.net.http.HttpResponse<byte[]>,
 * exposes easy JSON deserialization via Jackson.
 */
public class HttpResponse {
    private final java.net.http.HttpResponse<byte[]> raw;
    private final JsonSerializer json;

    public HttpResponse(java.net.http.HttpResponse<byte[]> raw, JsonSerializer json) {
        this.raw = raw;
        this.json = json;
    }

    public int statusCode() {
        return raw.statusCode();
    }

    public HttpHeaders headers() {
        return raw.headers();
    }

    public String body() {
        return new String(raw.body(), StandardCharsets.UTF_8);
    }

    public <T> T as(Class<T> cls) throws IOException {
        return json.fromJson(raw.body(), cls);
    }

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

    public <T> T as(TypeReference<T> type) throws IOException {
        return json.fromJson(raw.body(), type);
    }
}
