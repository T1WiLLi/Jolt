package ca.jolt.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.files.JoltFile;

public final class Response {
    private final HttpResponse<byte[]> httpResponse;
    private final ObjectMapper mapper;

    public Response(HttpResponse<byte[]> httpResponse, ObjectMapper mapper) {
        this.httpResponse = httpResponse;
        this.mapper = mapper;
    }

    public int status() {
        return httpResponse.statusCode();
    }

    public String header(String name) {
        return httpResponse.headers().firstValue(name).orElse("");
    }

    public Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        httpResponse.headers().map().forEach((key, values) -> {
            if (!values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        });
        return headers;
    }

    public String text() {
        return new String(httpResponse.body(), StandardCharsets.UTF_8);
    }

    public byte[] bytes() {
        return httpResponse.body();
    }

    public <T> T json(Class<T> clazz) throws IOException {
        return mapper.readValue(httpResponse.body(), clazz);
    }

    public <T> T json(TypeReference<T> typeReference) throws IOException {
        return mapper.readValue(httpResponse.body(), typeReference);
    }

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

    public boolean isSuccessful() {
        int code = status();
        return code >= 200 && code < 300;
    }
}
