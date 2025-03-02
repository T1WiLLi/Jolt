package ca.jolt.routing.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.exceptions.JoltBadRequestException;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.files.JoltFile;
import ca.jolt.form.Form;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.builder.CookieBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Provides an HTTP context object that encapsulates request and response
 * handling
 * within the Jolt framework. It includes utility methods for:
 * <ul>
 * <li>Retrieving path parameters, query parameters, headers, and the raw
 * request body.</li>
 * <li>Parsing JSON request bodies into Java objects.</li>
 * <li>Building and adding cookies to responses.</li>
 * <li>Returning text, JSON, or HTML responses.</li>
 * <li>Constructing {@link Form} objects from query parameters or the request
 * body.</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example :</strong>
 * </p>
 * 
 * <pre>{@code
 * get("/user/{id:int}", ctx -> ctx.html("Hi user #" + ctx.path("id").asInt()));
 * }</pre>
 *
 * @see Form
 * @see CookieBuilder
 * @see PathContextValue
 * @see QueryContextValue
 * @see HttpServletRequest
 * @see HttpServletResponse
 * @author William Beaudin
 * @since 1.0
 */
public final class JoltHttpContext {

    /**
     * JSON parser for reading and writing JSON in request/response.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final HttpServletRequest req;
    private final HttpServletResponse res;

    /**
     * A map of named path parameters extracted from the route path pattern.
     */
    private final Map<String, String> pathParams;

    /**
     * Constructs a {@code JoltHttpContext} with the specified request, response,
     * path parameter matcher, and parameter names.
     *
     * @param req
     *                    The underlying {@link HttpServletRequest}.
     * @param res
     *                    The underlying {@link HttpServletResponse}.
     * @param pathMatcher
     *                    A {@link Matcher} used to extract path parameters from the
     *                    URL
     *                    based on a route pattern.
     * @param paramNames
     *                    The list of named parameters extracted from the route
     *                    definition.
     */
    public JoltHttpContext(HttpServletRequest req, HttpServletResponse res,
            Matcher pathMatcher, List<String> paramNames) {
        this.req = req;
        this.res = res;
        this.pathParams = extractPathParams(pathMatcher, paramNames);
    }

    // -------------------------------------------------------
    // Request (Input)
    // -------------------------------------------------------

    /**
     * Returns the raw {@link HttpServletRequest} object for lower-level access.
     *
     * @return
     *         The raw servlet request.
     */
    public HttpServletRequest getRequest() {
        return req;
    }

    /**
     * Retrieves the HTTP method (e.g., GET, POST, PUT) of the current request.
     *
     * @return
     *         A string representing the HTTP method.
     */
    public String method() {
        return req.getMethod();
    }

    /**
     * Retrieves the request path (e.g., "/users/123"). It checks
     * {@link HttpServletRequest#getPathInfo()} and falls back to
     * {@link HttpServletRequest#getServletPath()} if {@code getPathInfo()} is null.
     *
     * @return
     *         The normalized request path, never empty.
     */
    public String requestPath() {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    /**
     * Returns a {@link PathContextValue} for a named path parameter.
     * <p>
     * For example, if the route is "/user/{id:int}" and the request
     * path is "/user/42", calling {@code ctx.path("id")} would return "42".
     * </p>
     *
     * @param name
     *             The name of the path parameter.
     * @return
     *         A {@link PathContextValue} (wrapping an optional string).
     */
    public PathContextValue path(String name) {
        return new PathContextValue(pathParams.get(name));
    }

    /**
     * Returns a {@link QueryContextValue} for a named query parameter.
     * <p>
     * If no query parameter with that name exists, this will wrap
     * an empty optional.
     * </p>
     *
     * @param name
     *             The name of the query parameter.
     * @return
     *         A {@link QueryContextValue} object.
     */
    public QueryContextValue query(String name) {
        return new QueryContextValue(req.getParameter(name));
    }

    /**
     * Returns a map of all query parameters, where each key is the parameter name
     * and the value is a list of strings (e.g., multiple values for the same
     * parameter).
     *
     * @return
     *         A map of query parameters, possibly empty if no query parameters
     *         exist.
     */
    public Map<String, List<String>> query() {
        Map<String, List<String>> map = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> map.put(key, Arrays.asList(values)));
        return map;
    }

    /**
     * Reads and returns the raw request body as a string.
     * If reading the body fails, a {@link JoltBadRequestException} is thrown.
     *
     * @return
     *         The raw request body content as a string.
     * @throws JoltBadRequestException
     *                                 If an I/O error occurs while reading.
     */
    public String bodyRaw() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to read request body: " + e.getMessage());
        }
        return sb.toString().trim();
    }

    /**
     * Parses the request body as JSON into an object of type {@code T}.
     * <p>
     * Returns {@code null} if the raw body is empty.
     * </p>
     *
     * @param <T>
     *             The type into which the JSON should be deserialized.
     * @param type
     *             A {@link Class} object for the target type.
     * @return
     *         The deserialized object, or {@code null} if empty.
     * @throws JoltBadRequestException
     *                                 If parsing fails.
     */
    public <T> T body(Class<T> type) {
        try {
            String raw = bodyRaw();
            if (raw.isEmpty()) {
                return null;
            }
            return JSON_MAPPER.readValue(raw, type);
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
        }
    }

    /**
     * Parses the request body as JSON into an object using a Jackson
     * {@link TypeReference}.
     * <p>
     * Returns {@code null} if the raw body is empty.
     * </p>
     *
     * @param <T>
     *                The generic type corresponding to the {@code TypeReference}.
     * @param typeRef
     *                A {@link TypeReference} describing the target type.
     * @return
     *         The deserialized object, or {@code null} if empty.
     * @throws JoltBadRequestException
     *                                 If parsing fails.
     */
    public <T> T body(TypeReference<T> typeRef) {
        String raw = bodyRaw();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(raw, typeRef);
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
        }
    }

    /**
     * Returns a list of all uploaded files as JoltFile objects, storing file data
     * in memory. It also automatically filters out empty files.
     * If the request isn't multipart, returns an empty list or throws an exception
     * as needed.
     * 
     * @return List<JoltFile> of JoltFile objects.
     */
    public List<JoltFile> getFiles() {
        List<JoltFile> files = new ArrayList<>();
        try {
            Collection<Part> parts = req.getParts();
            if (parts != null) {
                for (Part part : parts) {
                    String submittedFileName = part.getSubmittedFileName();
                    if (submittedFileName != null && !submittedFileName.trim().isEmpty()) {
                        byte[] data = part.getInputStream().readAllBytes();
                        if (data.length > 0) {
                            files.add(new JoltFile(
                                    submittedFileName,
                                    part.getContentType(),
                                    data.length,
                                    data));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new JoltBadRequestException("Failed to retrieve uploaded files: " + e.getMessage());
        }
        return files;
    }

    /**
     * Retrieves a specified HTTP header value from the request.
     *
     * @param n
     *          The name of the header.
     * @return
     *         The header value, or {@code null} if it does not exist.
     */
    public String header(String n) {
        return req.getHeader(n);
    }

    // -------------------------------------------------------
    // Response (Output)
    // -------------------------------------------------------

    /**
     * Returns the raw {@link HttpServletResponse} object for lower-level access.
     *
     * @return
     *         The raw servlet response.
     */
    public HttpServletResponse getResponse() {
        return res;
    }

    /**
     * Sets the HTTP status code on the response using a {@link HttpStatus} enum
     * constant.
     *
     * @param status
     *               The {@link HttpStatus} to set on the response.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     */
    public JoltHttpContext status(HttpStatus status) {
        res.setStatus(status.code());
        return this;
    }

    /**
     * Sets the HTTP status code on the response directly using an int.
     *
     * @param code
     *             The numeric status code (e.g., 200, 404).
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     */
    public JoltHttpContext status(int code) {
        res.setStatus(code);
        return this;
    }

    /**
     * Sets a header on the response.
     *
     * @param name
     *              The header name.
     * @param value
     *              The header value.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     */
    public JoltHttpContext header(String name, String value) {
        res.setHeader(name, value);
        return this;
    }

    /**
     * Writes a plain-text response.
     * <p>
     * Sets the Content-Type to {@code "text/plain;charset=UTF-8"}.
     * </p>
     *
     * @param data
     *             The text to write.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltHttpContext text(String data) {
        try {
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write(data);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing text response", e);
        }
        return this;
    }

    /**
     * Writes a JSON response using a {@link Map}.
     * <p>
     * Sets the Content-Type to {@code "application/json;charset=UTF-8"}.
     * </p>
     *
     * @param json
     *             A {@link Map} to serialize as JSON.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltHttpContext json(Map<String, Object> json) {
        try {
            res.setContentType("application/json;charset=UTF-8");
            JSON_MAPPER.writeValue(res.getWriter(), json);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing JSON response", e);
        }
        return this;
    }

    /**
     * Writes a JSON response from any Java object (POJO, List, etc.).
     * <p>
     * Sets the Content-Type to {@code "application/json;charset=UTF-8"}.
     * </p>
     *
     * @param data
     *             The Java object to serialize as JSON.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltHttpContext json(Object data) {
        try {
            res.setContentType("application/json;charset=UTF-8");
            JSON_MAPPER.writeValue(res.getWriter(), data);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing JSON response", e);
        }
        return this;
    }

    /**
     * Writes an HTML response.
     * <p>
     * Sets the Content-Type to {@code "text/html;charset=UTF-8"}.
     * </p>
     *
     * @param html
     *             The HTML content to write.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltHttpContext html(String html) {
        try {
            res.setContentType("text/html;charset=UTF-8");
            res.getWriter().write(html);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing HTML response", e);
        }
        return this;
    }

    /**
     * Retrieves a cookie from the request by name.
     *
     * @param name
     *             The cookie name to look up.
     * @return
     *         The matching {@link Cookie}, or {@code null} if not found.
     */
    public Cookie getCookie(String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * Returns a {@link CookieBuilder} preconfigured to attach cookies to this
     * response.
     * <p>
     * Example:
     * </p>
     * 
     * <pre>{@code
     * ctx.addCookie()
     *         .setName("sessionId")
     *         .setValue("abc123")
     *         .httpOnly(true)
     *         .build();
     * }</pre>
     *
     * @return
     *         A new {@link CookieBuilder} for building and adding cookies.
     */
    public CookieBuilder addCookie() {
        return new CookieBuilder(this.res);
    }

    /**
     * Returns a list of all cookies present in the request.
     *
     * @return
     *         A list of cookies, or an empty list if none are present.
     */
    public List<Cookie> getCookies() {
        return req.getCookies() != null ? Arrays.asList(req.getCookies()) : Collections.emptyList();
    }

    /**
     * Removes (expires) a cookie by name by setting its {@code maxAge} to 0
     * and re-adding it to the response.
     *
     * @param name
     *             The cookie name to remove.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     */
    public JoltHttpContext removeCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
        return this;
    }

    /**
     * Creates a new {@link Form} object by populating fields from query parameters.
     * <p>
     * Optionally, you can exclude specific parameter names if needed.
     * </p>
     *
     * @param excludes
     *                 Field names to exclude from the form.
     * @return
     *         A new {@link Form} populated with query parameter values.
     */
    public Form queryToForm(String... excludes) {
        Map<String, String> formData = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> {
            if (!shouldExclude(key, excludes) && values.length > 0) {
                formData.put(key, values[0]);
            }
        });
        return new Form(formData);
    }

    /**
     * Creates a new {@link Form} object by reading and parsing the request body.
     * <ul>
     * <li>If {@code contentType} is JSON, reads the body as JSON into key/value
     * pairs.</li>
     * <li>Otherwise, reads from normal form parameters.</li>
     * </ul>
     * <p>
     * Any excluded field names are not added to the resulting form.
     * </p>
     *
     * @param excludes
     *                 Field names to exclude from the form.
     * @return
     *         A new {@link Form} populated with data from the request body.
     * @throws JoltBadRequestException
     *                                 If JSON parsing fails.
     */
    public Form bodyToForm(String... excludes) {
        Map<String, String> formData = new HashMap<>();
        String contentType = req.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            String raw = bodyRaw();
            if (!raw.isEmpty()) {
                try {
                    Map<String, Object> parsed = JSON_MAPPER.readValue(raw, new TypeReference<Map<String, Object>>() {
                    });
                    for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                        String key = entry.getKey();
                        if (!shouldExclude(key, excludes)) {
                            Object valueObj = entry.getValue();
                            String valueStr = (valueObj == null) ? "" : valueObj.toString();
                            formData.put(key, valueStr);
                        }
                    }
                } catch (IOException e) {
                    throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
                }
            }
        } else {
            req.getParameterMap().forEach((key, values) -> {
                if (!shouldExclude(key, excludes) && values.length > 0) {
                    formData.put(key, values[0]);
                }
            });
        }
        return new Form(formData);
    }

    /**
     * Determines if a key should be excluded given the specified exclude array.
     */
    private boolean shouldExclude(String key, String[] excludes) {
        for (String exclude : excludes) {
            if (exclude.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts path parameters from the given {@link Matcher} and parameter names.
     */
    private Map<String, String> extractPathParams(Matcher matcher, List<String> paramNames) {
        Map<String, String> params = new HashMap<>();
        if (matcher != null && matcher.groupCount() > 0 && paramNames != null) {
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), matcher.group(i + 1));
            }
        }
        return params;
    }
}