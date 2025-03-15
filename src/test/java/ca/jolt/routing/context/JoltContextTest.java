package ca.jolt.routing.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.type.TypeReference;

import ca.jolt.cookie.CookieBuilder;
import ca.jolt.exceptions.JoltBadRequestException;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.form.Form;
import ca.jolt.http.HttpStatus;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JoltContextTest {

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private JoltContext context;
    private StringWriter responseWriter;
    private Matcher mockMatcher;
    private List<String> paramNames;

    @BeforeEach
    void setUp() throws Exception {
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);

        // Setup response writer for immediate writing
        responseWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseWriter, true);
        when(mockResponse.getWriter()).thenReturn(writer);

        // Setup path parameters using a simple regex
        Pattern pattern = Pattern.compile("/user/([^/]+)");
        mockMatcher = pattern.matcher("/user/123");
        mockMatcher.find(); // Set up the matcher groups
        paramNames = Arrays.asList("id");

        // Create context using the new immediate execution version.
        context = new JoltContext(mockRequest, mockResponse, mockMatcher, paramNames);
    }

    @Test
    void testGetRequest() {
        assertEquals(mockRequest, context.getRequest());
    }

    @Test
    void testGetResponse() {
        assertEquals(mockResponse, context.getResponse());
    }

    @Test
    void testMethod() {
        when(mockRequest.getMethod()).thenReturn("GET");
        assertEquals("GET", context.method());
    }

    @Test
    void testRequestPath_WithPathInfo() {
        when(mockRequest.getPathInfo()).thenReturn("/user/123");
        assertEquals("/user/123", context.requestPath());
    }

    @Test
    void testRequestPath_WithServletPath() {
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getServletPath()).thenReturn("/user/123");
        assertEquals("/user/123", context.requestPath());
    }

    @Test
    void testRequestPath_EmptyPath() {
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getServletPath()).thenReturn("");
        assertEquals("/", context.requestPath());
    }

    @Test
    void testClientIp_FromXForwardedFor() {
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        assertEquals("192.168.1.1", context.clientIp());
    }

    @Test
    void testClientIp_FromXRealIp() {
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(mockRequest.getHeader("X-Real-IP")).thenReturn("192.168.1.2");
        assertEquals("192.168.1.2", context.clientIp());
    }

    @Test
    void testClientIp_FromRemoteAddr() {
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.3");
        assertEquals("192.168.1.3", context.clientIp());
    }

    @Test
    void testUserAgent() {
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        assertEquals("Mozilla/5.0", context.userAgent());
    }

    @Test
    void testPath() {
        // The matcher set up in setUp() should extract "123" for the "id" parameter.
        assertEquals("123", context.path("id").get());
    }

    @Test
    void testQuery_SingleParam() {
        when(mockRequest.getParameter("page")).thenReturn("2");
        assertEquals("2", context.query("page").get());
        assertEquals(2, context.query("page").asIntOrDefault(-1));
    }

    @Test
    void testQuery_AllParams() {
        Map<String, String[]> parameterMap = Map.of(
                "page", new String[] { "2" },
                "limit", new String[] { "10" });
        when(mockRequest.getParameterMap()).thenReturn(parameterMap);

        Map<String, List<String>> result = context.queries();
        assertEquals(2, result.size());
        assertEquals(List.of("2"), result.get("page"));
        assertEquals(List.of("10"), result.get("limit"));
    }

    @Test
    void testBearerToken_Present() {
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer abc123token");
        Optional<String> token = context.bearerToken();
        assertTrue(token.isPresent());
        assertEquals("abc123token", token.get());
    }

    @Test
    void testBearerToken_NotPresent() {
        when(mockRequest.getHeader("Authorization")).thenReturn(null);
        Optional<String> token = context.bearerToken();
        assertFalse(token.isPresent());
    }

    @Test
    void testBearerToken_WrongFormat() {
        when(mockRequest.getHeader("Authorization")).thenReturn("Basic abc123");
        Optional<String> token = context.bearerToken();
        assertFalse(token.isPresent());
    }

    @Test
    void testBodyRaw() throws IOException {
        String requestBody = "{\"name\":\"John\",\"age\":30}";
        BufferedReader reader = new BufferedReader(new StringReader(requestBody));
        when(mockRequest.getReader()).thenReturn(reader);

        String result = context.bodyRaw();
        assertEquals(requestBody, result);
    }

    @Test
    void testBodyRaw_IoException() throws IOException {
        when(mockRequest.getReader()).thenThrow(new IOException("Mock IO error"));

        JoltBadRequestException exception = assertThrows(JoltBadRequestException.class, () -> {
            context.bodyRaw();
        });
        assertTrue(exception.getMessage().contains("Failed to read request body"));
    }

    @Test
    void testBody_WithClass() throws IOException {
        String jsonBody = "{\"name\":\"John\",\"age\":30}";
        BufferedReader reader = new BufferedReader(new StringReader(jsonBody));
        when(mockRequest.getReader()).thenReturn(reader);

        TestUser user = context.body(TestUser.class);
        assertNotNull(user);
        assertEquals("John", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void testBody_WithTypeReference() throws IOException {
        String jsonBody = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        BufferedReader reader = new BufferedReader(new StringReader(jsonBody));
        when(mockRequest.getReader()).thenReturn(reader);

        List<TestUser> users = context.body(new TypeReference<List<TestUser>>() {
        });
        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("John", users.get(0).getName());
        assertEquals(25, users.get(1).getAge());
    }

    @Test
    void testHeader() {
        when(mockRequest.getHeader("Content-Type")).thenReturn("application/json");
        assertEquals("application/json", context.getHeader("Content-Type"));
    }

    @Test
    void testGetCookie() {
        Cookie[] cookies = {
                new Cookie("session", "abc123"),
                new Cookie("preference", "dark-mode")
        };
        when(mockRequest.getCookies()).thenReturn(cookies);

        Cookie result = context.getCookie("session");
        assertNotNull(result);
        assertEquals("session", result.getName());
        assertEquals("abc123", result.getValue());
    }

    @Test
    void testAddCookie() {
        CookieBuilder builder = context.addCookie();
        assertNotNull(builder);
    }

    @Test
    void testGetCookies() {
        Cookie[] cookies = {
                new Cookie("session", "abc123"),
                new Cookie("preference", "dark-mode")
        };
        when(mockRequest.getCookies()).thenReturn(cookies);

        List<Cookie> result = context.getCookies();
        assertEquals(2, result.size());
    }

    @Test
    void testRemoveCookie() {
        context.removeCookie("session");
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(mockResponse).addCookie(cookieCaptor.capture());

        Cookie removedCookie = cookieCaptor.getValue();
        assertEquals("session", removedCookie.getName());
        assertEquals(0, removedCookie.getMaxAge());
    }

    @Test
    void testBuildForm() throws IOException {
        // Setup query parameters
        Map<String, String[]> parameterMap = Map.of(
                "name", new String[] { "John" },
                "email", new String[] { "john@example.com" });
        when(mockRequest.getParameterMap()).thenReturn(parameterMap);

        // Setup JSON body parameters
        when(mockRequest.getContentType()).thenReturn("application/json");
        String jsonBody = "{\"age\":30,\"city\":\"New York\"}";
        BufferedReader reader = new BufferedReader(new StringReader(jsonBody));
        when(mockRequest.getReader()).thenReturn(reader);

        Form form = context.buildForm();
        assertEquals("123", form.getValue("id")); // from path parameter
        assertEquals("John", form.getValue("name")); // from query
        assertEquals("john@example.com", form.getValue("email")); // from query
        assertEquals("30", form.getValue("age")); // from JSON body
        assertEquals("New York", form.getValue("city")); // from JSON body
    }

    @Test
    void testBuildForm_WithExcludes() throws IOException {
        // Setup query parameters
        Map<String, String[]> parameterMap = Map.of(
                "name", new String[] { "John" },
                "email", new String[] { "john@example.com" },
                "password", new String[] { "secret" });
        when(mockRequest.getParameterMap()).thenReturn(parameterMap);

        Form form = context.buildForm("password", "email");
        assertEquals("John", form.getValue("name"));
        assertNull(form.getValue("password")); // excluded
        assertNull(form.getValue("email")); // excluded
    }

    @Test
    void testAbort() {
        JoltHttpException exception = assertThrows(JoltHttpException.class, () -> {
            context.abort(HttpStatus.NOT_FOUND, "Resource not found");
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Resource not found", exception.getMessage());
    }

    @Test
    void testAbortNotFound() {
        JoltHttpException exception = assertThrows(JoltHttpException.class, () -> {
            context.abortNotFound("User not found");
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found", exception.getMessage());
    }

    // Helper class for testing JSON conversion
    static class TestUser {
        private String name;
        private int age;

        public TestUser() {
        }

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
