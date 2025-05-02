package io.github.t1willi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpStatusTests {
    @Test
    @DisplayName("Test OK status code and reason")
    void testOkStatus() {
        HttpStatus status = HttpStatus.OK;
        assertEquals(200, status.code(), "OK status code should be 200");
        assertEquals("OK", status.reason(), "OK reason should be 'OK'");
        assertEquals("200 OK", status.toString(), "OK toString should be '200 OK'");
        assertEquals(HttpStatus.StatusCategory.SUCCESS, status.category(), "OK category should be SUCCESS");
    }

    @Test
    @DisplayName("Test NOT_FOUND status code and reason")
    void testNotFoundStatus() {
        HttpStatus status = HttpStatus.NOT_FOUND;
        assertEquals(404, status.code(), "NOT_FOUND status code should be 404");
        assertEquals("Not Found", status.reason(), "NOT_FOUND reason should be 'Not Found'");
        assertEquals("404 Not Found", status.toString(), "NOT_FOUND toString should be '404 Not Found'");
        assertEquals(HttpStatus.StatusCategory.CLIENT_ERROR, status.category(),
                "NOT_FOUND category should be CLIENT_ERROR");
    }

    @Test
    @DisplayName("Test fromCode with known codes")
    void testFromCodeKnown() {
        assertEquals(HttpStatus.OK, HttpStatus.fromCode(200), "fromCode(200) should return OK");
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.fromCode(404), "fromCode(404) should return NOT_FOUND");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.fromCode(500),
                "fromCode(500) should return INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("Test fromCode with unknown code")
    void testFromCodeUnknown() {
        assertEquals(HttpStatus.NOT_IMPLEMENTED, HttpStatus.fromCode(999),
                "fromCode(999) should return NOT_IMPLEMENTED");
    }

    @Test
    @DisplayName("Test category for all ranges")
    void testCategoryRanges() {
        assertEquals(HttpStatus.StatusCategory.SUCCESS, HttpStatus.OK.category(), "200 should be SUCCESS");
        assertEquals(HttpStatus.StatusCategory.REDIRECTION, HttpStatus.FOUND.category(), "302 should be REDIRECTION");
        assertEquals(HttpStatus.StatusCategory.CLIENT_ERROR, HttpStatus.BAD_REQUEST.category(),
                "400 should be CLIENT_ERROR");
        assertEquals(HttpStatus.StatusCategory.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.category(),
                "500 should be SERVER_ERROR");
    }
}
