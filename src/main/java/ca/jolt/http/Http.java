package ca.jolt.http;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class Http {
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private Http() {
        // Prevent instantiation
    }

    public static ObjectMapper defaultMapper() {
        return DEFAULT_MAPPER;
    }

    public static RequestBuilder get(String url) {
        return new RequestBuilder(HttpMethod.GET, url);
    }

    public static RequestBuilder post(String url) {
        return new RequestBuilder(HttpMethod.POST, url);
    }

    public static RequestBuilder put(String url) {
        return new RequestBuilder(HttpMethod.PUT, url);
    }

    public static RequestBuilder delete(String url) {
        return new RequestBuilder(HttpMethod.DELETE, url);
    }
}
