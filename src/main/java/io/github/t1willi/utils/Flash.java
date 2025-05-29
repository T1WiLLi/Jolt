package io.github.t1willi.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltDispatcherServlet;

/**
 * A utility class for managing Flash messages during view rendering.
 * <p>
 * The Flash class provides a mechanism to store and retrieve a single message
 * along with its type at any given time. Once consumed, the message is cleared,
 * ensuring that it is only displayed once per rendering cycle. This prevents
 * repeated display of the same message when a view is refreshed multiple times.
 * <p>
 * By default, the Flash class is accessible within views using the
 * {@code Flash}
 * object. The message can be retrieved using {@code Flash.message()}, while
 * {@code Flash.type()} provides the corresponding message type. You can also
 * use {@code Flash.has()} to check if any message is currently stored. Which
 * can be quite helpful when making logic decisions in your views.
 * <p>
 * Supported message types include:
 * <ul>
 * <li>{@code SUCCESS} - Indicates a successful operation.</li>
 * <li>{@code ERROR} - Represents an error or failure.</li>
 * <li>{@code WARNING} - Highlights a cautionary message.</li>
 * <li>{@code INFO} - Conveys informational content.</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>
 * {@code
 * Flash.success("Operation completed successfully!");
 * ... In your view ...
 * {@literal <p class="${flash.type()}}">${flash.message()}{@literal</p>}
 * }
 * </pre>
 * <p>
 * This class employs {@link ThreadLocal} storage to manage messages at a thread
 * level,
 * ensuring thread safety in a multi-threaded environment.
 * 
 * @implNote Flash messages are stored in Javascript-accessible cookies, which
 *           facilitate integration with both SSR and CSR. The cookie name is
 *           always the same {@code "flash_message"} and {@code "flash_type"}
 *           for the type of the Flash. Making it easy to remember.
 * 
 * @since 2.8.9
 * @author William Beaudin
 */
public final class Flash {

    private static final String COOKIE_MESSAGE = "flash_message";
    private static final String COOKIE_TYPE = "flash_type";

    public static void success(String message) {
        set(message, MessageType.SUCCESS);
    }

    public static void error(String message) {
        set(message, MessageType.ERROR);
    }

    public static void info(String message) {
        set(message, MessageType.INFO);
    }

    public static void warning(String message) {
        set(message, MessageType.WARNING);
    }

    public static String message() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        Optional<String> raw = ctx.cookieValue(COOKIE_MESSAGE);
        ctx.removeCookie(COOKIE_MESSAGE);
        ctx.removeCookie(COOKIE_TYPE);
        return raw.map(Flash::decode).orElse(null);
    }

    public static String type() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        return ctx.cookieValue(COOKIE_TYPE)
                .map(Flash::decode)
                .orElse(null);
    }

    public static boolean has() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        return ctx.cookieValue(COOKIE_MESSAGE).isPresent();
    }

    public static void clear() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        ctx.removeCookie(COOKIE_MESSAGE);
        ctx.removeCookie(COOKIE_TYPE);
    }

    private static void set(String message, MessageType type) {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        String encMsg = encode(message);
        String encType = encode(type.toString());
        ctx.addCookie().unsecureCookie(COOKIE_MESSAGE, encMsg);
        ctx.addCookie().unsecureCookie(COOKIE_TYPE, encType);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private Flash() {
        // No operation (utility class)
    }

    /**
     * Enum representing different message types.
     */
    private static enum MessageType {
        SUCCESS, ERROR, WARNING, INFO;

        /**
         * Returns the lowercase string representation of the message type.
         *
         * @return the name of the enum in lowercase
         */
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}