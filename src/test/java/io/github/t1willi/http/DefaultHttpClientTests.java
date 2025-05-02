package io.github.t1willi.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.t1willi.http.api.HttpClient.HttpRequest;
import io.github.t1willi.http.impl.DefaultHttpClient;
import io.github.t1willi.http.json.JsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Unit tests for the DefaultHttpClient class.
 */
@SuppressWarnings("unchecked")
public class DefaultHttpClientTests {

    private DefaultHttpClient client;
    private java.net.http.HttpClient mockHttpClient;
    private JsonSerializer mockJsonSerializer;
    private HttpResponse<byte[]> mockResponse;

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        mockHttpClient = mock(java.net.http.HttpClient.class);
        mockJsonSerializer = mock(JsonSerializer.class);
        client = new DefaultHttpClient(mockHttpClient, mockJsonSerializer);

        mockResponse = (HttpResponse<byte[]>) mock(HttpResponse.class);

        when(mockHttpClient.<byte[]>send(
                any(java.net.http.HttpRequest.class),
                any(java.net.http.HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body())
                .thenReturn("{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8));
        when(mockResponse.headers())
                .thenReturn(HttpHeaders.of(
                        Map.of("Content-Type", List.of("application/json")),
                        (a, b) -> true));
    }

    @Test
    @DisplayName("Test async request initiation")
    public void testAsync() {
        CompletableFuture<HttpRequest> future = client.async(HttpMethod.GET, "http://example.com");

        assertNotNull(future, "async should return a CompletableFuture");
        assertTrue(future.isDone(), "Future should be completed");
        assertInstanceOf(
                HttpRequest.class,
                future.join(),
                "Future should contain an HttpRequest");
    }

    @Test
    @DisplayName("Test sync request initiation")
    public void testSync() {
        HttpRequest request = client.sync(HttpMethod.GET, "http://example.com");

        assertNotNull(request, "sync should return a HttpRequest");
        assertInstanceOf(
                HttpRequest.class,
                request,
                "Request should be an HttpRequest");
    }

    @Test
    @DisplayName("Test request with header")
    public void testWithHeader() throws IOException, InterruptedException {
        var request = client.sync(HttpMethod.GET, "http://example.com")
                .withHeader("Authorization", "Bearer token");

        assertEquals(200, request.status(), "Status should be 200");
        verify(mockHttpClient).send(
                argThat(req -> req.headers()
                        .firstValue("Authorization")
                        .orElse("")
                        .equals("Bearer token")),
                any());
    }

    @Test
    @DisplayName("Test request with body")
    public void testWithBody() throws IOException, InterruptedException {
        Object body = new TestObject("test");
        byte[] jsonBytes = "{\"name\":\"test\"}".getBytes();
        when(mockJsonSerializer.toJson(body)).thenReturn(jsonBytes);

        var request = client.sync(HttpMethod.POST, "http://example.com")
                .withBody(body);

        assertEquals(200, request.status(), "Status should be 200");
        verify(mockHttpClient).send(
                argThat(req -> req.headers()
                        .firstValue("Content-Type")
                        .orElse("")
                        .equals("application/json")),
                any());
    }

    @Test
    @DisplayName("Test request with timeout")
    public void testTimeout() throws IOException, InterruptedException {
        var request = client.sync(HttpMethod.GET, "http://example.com")
                .timeout(Duration.ofSeconds(5));

        assertEquals(200, request.status(), "Status should be 200");
        verify(mockHttpClient).send(
                argThat(req -> req.timeout()
                        .orElse(null)
                        .equals(Duration.ofSeconds(5))),
                any());
    }

    @Test
    @DisplayName("Test response as Class")
    public void testAsClass() throws IOException, InterruptedException {
        TestObject expected = new TestObject("test");
        when(mockJsonSerializer.fromJson(
                any(byte[].class), eq(TestObject.class)))
                .thenReturn(expected);

        var request = client.sync(HttpMethod.GET, "http://example.com");
        TestObject result = request.as(TestObject.class);

        assertEquals(expected, result, "as(Class) should return deserialized object");
    }

    @Test
    @DisplayName("Test response as List")
    public void testAsList() throws IOException, InterruptedException {
        List<TestObject> expected = List.of(new TestObject("test"));
        // <-- use a typed any() matcher for TypeReference<List<TestObject>>
        when(mockJsonSerializer.fromJson(
                any(byte[].class),
                ArgumentMatchers.<TypeReference<List<TestObject>>>any())).thenReturn(expected);

        var request = client.sync(HttpMethod.GET, "http://example.com");
        List<TestObject> result = request.asList(TestObject.class);

        assertEquals(expected, result, "asList should return deserialized list");
    }

    @Test
    @DisplayName("Test response as String")
    public void testAsString() throws IOException, InterruptedException {
        var request = client.sync(HttpMethod.GET, "http://example.com");
        String result = request.asString();
        assertEquals("{\"name\":\"test\"}", result, "asString should return response body");
    }

    @Test
    @DisplayName("Test writeTo Writer")
    public void testWriteToWriter() throws IOException, InterruptedException {
        StringWriter writer = new StringWriter();
        var request = client.sync(HttpMethod.GET, "http://example.com");
        request.writeTo(writer);
        assertEquals(
                "{\"name\":\"test\"}",
                writer.toString(),
                "writeTo(Writer) should write response body");
    }

    @Test
    @DisplayName("Test writeTo OutputStream")
    public void testWriteToOutputStream() throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var request = client.sync(HttpMethod.GET, "http://example.com");
        request.writeTo(out);
        assertArrayEquals(
                "{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8),
                out.toByteArray(),
                "writeTo(OutputStream) should write response body");
    }

    private static class TestObject {
        private String name;

        public TestObject(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestObject))
                return false;
            return name.equals(((TestObject) o).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
