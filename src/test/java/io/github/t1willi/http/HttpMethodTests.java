package io.github.t1willi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpMethodTests {
    @Test
    @DisplayName("Test fromString with valid methods")
    void testFromStringValid() {
        assertEquals(HttpMethod.GET, HttpMethod.fromString("GET"), "fromString('GET') should return GET");
        assertEquals(HttpMethod.POST, HttpMethod.fromString("POST"), "fromString('POST') should return POST");
        assertEquals(HttpMethod.PUT, HttpMethod.fromString("put"), "fromString('put') should be case-insensitive");
    }

    @Test
    @DisplayName("Test fromString with invalid method")
    void testFromStringInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            HttpMethod.fromString("INVALID");
        }, "fromString('INVALID') should throw IllegalArgumentException");
        assertEquals("Unknown HTTP method: INVALID", exception.getMessage());
    }

    @Test
    @DisplayName("Test toString for all methods")
    void testToString() {
        assertEquals("GET", HttpMethod.GET.toString(), "GET toString should return 'GET'");
        assertEquals("POST", HttpMethod.POST.toString(), "POST toString should return 'POST'");
        assertEquals("DELETE", HttpMethod.DELETE.toString(), "DELETE toString should return 'DELETE'");
    }
}
