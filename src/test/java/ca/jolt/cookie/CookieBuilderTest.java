package ca.jolt.cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ca.jolt.injector.JoltContainer;

/**
 * Unit tests for {@link CookieBuilder}.
 */
public class CookieBuilderTest {

    private HttpServletResponse response;
    private TestCookieConfiguration testConfig;
    private MockedStatic<JoltContainer> mockedJoltContainer;
    private JoltContainer dummyContainer;

    @BeforeEach
    void setUp() {
        response = mock(HttpServletResponse.class);
        testConfig = new TestCookieConfiguration();
        dummyContainer = mock(JoltContainer.class);
        mockedJoltContainer = Mockito.mockStatic(JoltContainer.class);
        mockedJoltContainer.when(JoltContainer::getInstance).thenReturn(dummyContainer);
        when(dummyContainer.getBean(CookieConfiguration.class)).thenReturn(testConfig);
    }

    @AfterEach
    void tearDown() {
        mockedJoltContainer.close();
    }

    @Test
    void testBuildThrowsExceptionWhenNameIsNull() {
        CookieBuilder builder = new CookieBuilder(response);
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Cookie name may not be null or empty", exception.getMessage());
    }

    @Test
    void testBuildThrowsExceptionWhenNameIsEmpty() {
        CookieBuilder builder = new CookieBuilder(response);
        builder.setName("");
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Cookie name may not be null or empty", exception.getMessage());
    }

    @Test
    void testBuildAddsCookieToResponse() {
        CookieBuilder builder = new CookieBuilder(response);
        builder.setName("testCookie")
                .setValue("testValue")
                .httpOnly(true)
                .secure(true)
                .maxAge(3600)
                .path("/test")
                .domain("example.com")
                .sameSite("Strict")
                .build();

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();

        assertEquals("testCookie", cookie.getName());
        assertEquals("testValue", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals(3600, cookie.getMaxAge());
        assertEquals("/test", cookie.getPath());
        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    void testSessionCookie() {
        CookieBuilder builder = new CookieBuilder(response);
        builder.sessionCookie("sessionValue");

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();

        assertEquals(testConfig.getSessionCookieName(), cookie.getName());
        assertEquals("sessionValue", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals(testConfig.getSessionCookiePath(), cookie.getPath());
    }

    @Test
    void testJwtCookie() {
        CookieBuilder builder = new CookieBuilder(response);
        builder.jwtCookie("jwtValue", 7200);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();

        assertEquals(testConfig.getJwtCookieName(), cookie.getName());
        assertEquals("jwtValue", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals(testConfig.getJwtCookiePath(), cookie.getPath());
        assertEquals(7200, cookie.getMaxAge());
    }

    @Test
    void testUnsecureCookie() {
        CookieBuilder builder = new CookieBuilder(response);
        builder.unsecureCookie("unsecure", "value");

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();

        assertEquals("unsecure", cookie.getName());
        assertEquals("value", cookie.getValue());
        assertFalse(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertEquals("/", cookie.getPath());
    }

    /**
     * Dummy implementation of {@link CookieConfiguration} for testing purposes.
     */
    private static class TestCookieConfiguration extends CookieConfiguration {
        @Override
        public String getSessionCookieName() {
            return "sessionTestName";
        }

        @Override
        public String getSessionCookiePath() {
            return "/sessionPath";
        }

        @Override
        public String getSessionSameSitePolicy() {
            return "Strict";
        }

        @Override
        public String getJwtCookieName() {
            return "jwtTestName";
        }

        @Override
        public String getJwtCookiePath() {
            return "/jwtPath";
        }

        @Override
        public String getJwtSameSitePolicy() {
            return "Lax";
        }

        @Override
        public CookieConfiguration configure() {
            return this;
        }
    }
}
