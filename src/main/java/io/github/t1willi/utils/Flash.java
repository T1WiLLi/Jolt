package io.github.t1willi.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltDispatcherServlet;

/**
 * Manages one‐off flash messages for a single request‐render cycle.
 * <p>
 * Flash messages are simultaneously stored in:
 * <ul>
 * <li>a ThreadLocal map (for immediate retrieval in the same render), and</li>
 * <li>HTTP cookies (to survive redirects and subsequent requests).</li>
 * </ul>
 * Once consumed via {@link #message()}, the flash entry is removed, ensuring it
 * appears only once per cycle.
 * <p>
 * **Usage (in controller):**
 * 
 * <pre>
 * Flash.success("Saved successfully!");
 * return render("viewName", model);
 * </pre>
 * 
 * **Usage (in template):**
 * 
 * <pre>
 *   <#if flash.has()>
 *     <div class="alert alert-${flash.type()}">${flash.message()}</div>
 *   </#if>
 * </pre>
 *
 * Supported types:
 * <ul>
 * <li>SUCCESS</li>
 * <li>ERROR</li>
 * <li>WARNING</li>
 * <li>INFO</li>
 * </ul>
 *
 * @since 2.8.10
 * @author William Beaudin
 */
public final class Flash {
    private static final String COOKIE_MESSAGE = "flash_message";
    private static final String COOKIE_TYPE = "flash_type";

    // Immediate access storage for the current thread
    private static final ThreadLocal<Map<String, String>> local = ThreadLocal.withInitial(HashMap::new);

    // Prevent instantiation
    private Flash() {
    }

    /**
     * Stores a success‐type flash message.
     * 
     * @param message the message to display
     */
    public static void success(String message) {
        set(message, MessageType.SUCCESS);
    }

    /**
     * Stores an error‐type flash message.
     * 
     * @param message the message to display
     */
    public static void error(String message) {
        set(message, MessageType.ERROR);
    }

    /**
     * Stores an informational flash message.
     * 
     * @param message the message to display
     */
    public static void info(String message) {
        set(message, MessageType.INFO);
    }

    /**
     * Stores a warning‐type flash message.
     * 
     * @param message the message to display
     */
    public static void warning(String message) {
        set(message, MessageType.WARNING);
    }

    /**
     * Retrieves and consumes the flash message text.
     * 
     * @return the message, or {@code null} if none
     */
    public static String message() {
        Map<String, String> map = local.get();
        if (map.containsKey(COOKIE_MESSAGE)) {
            String decoded = decode(map.remove(COOKIE_MESSAGE));
            return decoded;
        }
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        Optional<String> raw = ctx.cookieValue(COOKIE_MESSAGE);
        ctx.removeCookie(COOKIE_MESSAGE);
        return raw.map(Flash::decode).orElse(null);
    }

    /**
     * Returns the flash message type (e.g. "success", "error").
     * Consumes the type so it is only available once.
     * 
     * @return the type, or {@code null} if none
     */
    public static String type() {
        Map<String, String> map = local.get();
        if (map.containsKey(COOKIE_TYPE)) {
            return decode(map.remove(COOKIE_TYPE));
        }
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        Optional<String> raw = ctx.cookieValue(COOKIE_TYPE);
        ctx.removeCookie(COOKIE_TYPE);
        return raw.map(Flash::decode).orElse(null);
    }

    /**
     * Checks if any flash message is set for this render cycle or pending in
     * cookies.
     * 
     * @return {@code true} if a flash message exists
     */
    public static boolean has() {
        if (local.get().containsKey(COOKIE_MESSAGE)) {
            return true;
        }
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        return ctx.cookieValue(COOKIE_MESSAGE).isPresent();
    }

    /**
     * Clears any pending flash message from both ThreadLocal and cookies.
     */
    public static void clear() {
        local.get().clear();
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        ctx.removeCookie(COOKIE_MESSAGE);
        ctx.removeCookie(COOKIE_TYPE);
    }

    /**
     * Encodes and stores a flash message and its type.
     * 
     * @param message text to display
     * @param type    category of the message
     */
    private static void set(String message, MessageType type) {
        String encMsg = encode(message);
        String encType = encode(type.toString());
        Map<String, String> map = local.get();
        map.put(COOKIE_MESSAGE, encMsg);
        map.put(COOKIE_TYPE, encType);
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        ctx.addCookie().unsecureCookie(COOKIE_MESSAGE, encMsg);
        ctx.addCookie().unsecureCookie(COOKIE_TYPE, encType);
    }

    private static String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private static String decode(String val) {
        return URLDecoder.decode(val, StandardCharsets.UTF_8);
    }

    /**
     * Defines supported flash message categories.
     */
    private enum MessageType {
        SUCCESS, ERROR, WARNING, INFO;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
