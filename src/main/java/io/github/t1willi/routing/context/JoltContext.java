package io.github.t1willi.routing.context;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.cookie.CookieBuilder;
import io.github.t1willi.exceptions.JoltBadRequestException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.files.JoltFile;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.security.utils.InputSanitizer;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.utils.JacksonUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
public final class JoltContext {

    private final RequestContext requestContext;
    private final ResponseContext responseContext;
    private final TemplatingContext templatingContext;

    private boolean committed = false;

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
    public JoltContext(HttpServletRequest req, HttpServletResponse res,
            Matcher pathMatcher, List<String> paramNames) {
        this.requestContext = new RequestContext(req);
        this.responseContext = new ResponseContext(res);
        this.templatingContext = new TemplatingContext();
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
        return requestContext.getRequest();
    }

    /**
     * Retrieves the HTTP method (e.g., GET, POST, PUT) of the current request.
     *
     * @return
     *         A string representing the HTTP method.
     */
    public String method() {
        return requestContext.method();
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
        return requestContext.getPath();
    }

    /**
     * This function try to find and retrieve the client's IP address.
     * <p>
     * It checks the "X-Forwarded-For" header first, then the "X-Real-IP" header,
     * and finally if none are found,
     * it uses the {@link HttpServletRequest#getRemoteAddr()} method to get the IP
     * address of the client.
     * 
     * @return The client's IP address.
     */
    public String clientIp() {
        return requestContext.clientIp();
    }

    /**
     * Retrieve the value of the User-Agent header.
     * 
     * @return The client's user agent.
     */
    public String userAgent() {
        return getHeader("User-Agent");
    }

    /**
     * Returns a {@link PathContextValue} for a named path parameter.
     * <p>
     * For example, if the route is "/user/{id:int}" and the request
     * path is "/user/42", calling {@code ctx.path("id").asInt()} would return "42".
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
     *
     * @param name
     *             The name of the query parameter.
     * @return
     *         A {@link QueryContextValue} object.
     */
    public QueryContextValue query(String name) {
        return new QueryContextValue(requestContext.getParameter(name));
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
    public Map<String, List<String>> queries() {
        return requestContext.getAllQuery();
    }

    /**
     * Retrieves the bearer token from the HTTP context.
     * <p>
     * This method attempts to extract the bearer token from the HTTP request
     * headers.
     * The bearer token is typically used for authentication and authorization
     * purposes.
     * <p>
     * The method returns an {@link Optional} containing the bearer token if it is
     * present
     * in the request headers. If the bearer token is not found, an empty
     * {@link Optional}
     * is returned.
     * 
     * @return an {@link Optional} containing the bearer token if present, otherwise
     *         an empty {@link Optional}
     */
    public Optional<String> bearerToken() {
        return requestContext.bearerToken();
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
        return requestContext.bodyRaw();
    }

    /**
     * Parses the request body as JSON into an object of type {@code T}.
     * <p>
     * Returns {@code null} if the raw body is empty.
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
        return requestContext.body(type);
    }

    /**
     * Parses the request body as JSON into an object using a Jackson
     * {@link TypeReference}.
     * <p>
     * Returns {@code null} if the raw body is empty.
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
        return requestContext.body(typeRef);
    }

    /**
     * Returns a list of all uploaded files as JoltFile objects, storing file data
     * in memory. It also automatically filters out empty files.
     * If the request isn't multipart, returns an empty list or throws an exception
     * as needed.
     * 
     * @return List<{@link JoltFile}> of JoltFile objects.
     */
    public List<JoltFile> getFiles() {
        List<JoltFile> files = requestContext.getFiles();

        for (JoltFile file : files) {
            String originalFilename = file.getFileName();
            if (originalFilename != null) {
                file.setFileName(InputSanitizer.sanitizeFilename(originalFilename));
            }
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
    public String getHeader(String n) {
        return requestContext.getHeader(n);
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
        return responseContext.getResponse();
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
    public JoltContext status(HttpStatus status) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot set header after response has been committed");
        }
        responseContext.setStatus(status);
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
    public JoltContext status(int code) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot set header after response has been committed");
        }
        responseContext.setStatus(code);
        return this;
    }

    public HttpStatus getStatus() {
        return responseContext.getStatus();
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
    public JoltContext setHeader(String name, String value) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot set header after response has been committed");
        }
        responseContext.setHeader(name, value);
        return this;
    }

    /**
     * Redirect's the response to a new existing location.
     * 
     * @param location The new location to redirect to.
     * @return This {@code JoltHttpContext}, for fluent chaining.
     */
    public JoltContext redirect(String location) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot set header after response has been committed");
        }
        responseContext.sendRedirect(location, HttpStatus.FOUND.code());
        return this;
    }

    /**
     * Redirect's the response to the newly created resource.
     * 
     * @param location        The newly created resource.
     * @param redirectedRoute The newly created resource's route.
     * @return This {@code JoltHttpContext}, for fluent chaining.
     */
    public JoltContext redirect(String location, Runnable redirectedRoute) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot set header after response has been committed");
        }
        redirectedRoute.run();
        responseContext.sendRedirect(location, HttpStatus.FOUND.code());
        return this;
    }

    /**
     * Writes a plain-text response.
     * <p>
     * Sets the Content-Type to {@code "text/plain;charset=UTF-8"}.
     *
     * @param data
     *             The text to write.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltContext text(String data) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot write text after response has been committed");
        }
        responseContext.text(InputSanitizer.sanitizeString(data));
        return this;
    }

    /**
     * Writes a JSON response from any Java object (POJO, List, etc.).
     * <p>
     * Sets the Content-Type to {@code "application/json;charset=UTF-8"}.
     *
     * @param data
     *             The Java object to serialize as JSON.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltContext json(Object data) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot write JSON after response has been committed");
        }
        if (data instanceof Map) {
            Map<Object, Object> sanitizedMap = new HashMap<>();
            ((Map<?, ?>) data).forEach((key, value) -> {
                Object sanitizedValue = value;
                if (value instanceof String) {
                    sanitizedValue = InputSanitizer.sanitizeJsonString((String) value);
                }
                sanitizedMap.put(key, sanitizedValue);
            });
            responseContext.json(sanitizedMap);
        } else {
            responseContext.json(data);
        }
        return this;
    }

    /**
     * Writes an HTML response.
     * <p>
     * Sets the Content-Type to {@code "text/html;charset=UTF-8"}.
     *
     * @param html
     *             The HTML content to write.
     * @return
     *         This {@code JoltHttpContext}, for fluent chaining.
     * @throws JoltHttpException
     *                           If an I/O error occurs while writing.
     */
    public JoltContext html(String html) {
        if (committed) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot write HTML after response has been committed");
        }
        String sanitized = InputSanitizer.HTML_SCRIPT_PATTERN.matcher(html).replaceAll("");
        responseContext.html(sanitized);
        return this;
    }

    /**
     * Render a Freemarker template with the provided model. The template must be
     * located in the "resources/templates" directory in the classpath.
     * 
     * 
     * @param template The name of the template file to render.
     * @param model    The model to pass to the template.
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext render(String template, JoltModel model) {
        templatingContext.render(responseContext, template, model);
        return this;
    }

    /**
     * Write whatever data the object is composed of to the response.
     * This function doesn't sanitize the data, so use with caution.
     * 
     * @param data The data to write to the response.
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext write(Object data) {
        try {
            responseContext.write(data);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing response", e);
        }
        return this;
    }

    /**
     * Serves a static file from the "/static" directory located in the classpath.
     * <p>
     * For example, {@code ctx.serve("index.html")} will try to locate the resource
     * at "resources/static/index.html" and write its contents to the response with
     * an
     * appropriate MIME type.
     *
     * @param resource the file name to serve (e.g., "index.html" or
     *                 "image.png")
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext serve(String resource) {
        responseContext.serve(resource);
        return this;
    }

    /**
     * Triggers a file download.
     * Sets the Content-Disposition header so that the browser prompts a download.
     * 
     * @param file     The file to download
     * @param filename The filename to use for the download
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext download(JoltFile file, String filename) {
        responseContext.download(file, filename);
        return this;
    }

    /**
     * Sets the Content-Type header on the response.
     * 
     * @param type The content type to set.
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext contentType(String type) {
        responseContext.setContentType(type);
        return this;
    }

    /**
     * Shortand for HTTP Status 200 OK.
     * 
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext ok() {
        return status(HttpStatus.OK);
    }

    /**
     * Shortand for HTTP Status 201 Created.
     * 
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext created() {
        return status(HttpStatus.CREATED);
    }

    /**
     * Shortand for HTTP Status 204 No Content.
     * 
     * @return this {@code JoltHttpContext} for fluent chaining.
     */
    public JoltContext noContent() {
        return status(HttpStatus.NO_CONTENT);
    }

    /**
     * Aborts the request with a specified HTTP status and message.
     * Sets the response status and writes the message as plain text.
     * 
     * @param status  The HTTP status to set.
     * @param message The message to write.
     * @throws JoltHttpException If writing the response fails.
     */
    public JoltContext abort(HttpStatus status, String message) {
        throw new JoltHttpException(status, message);
    }

    /**
     * Aborts the request with 400 Bad Request status.
     *
     * @param message The error message to send
     * @throws JoltHttpException If writing the response fails
     */
    public JoltContext abortBadRequest(String message) {
        return abort(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Aborts the request with 404 Not Found status.
     *
     * @param message The error message to send
     * @throws JoltHttpException If writing the response fails
     */
    public JoltContext abortNotFound(String message) {
        return abort(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Aborts the request with 500 Internal Server Error status.
     *
     * @param message The error message to send
     * @throws JoltHttpException If writing the response fails
     */
    public JoltContext abortInternalServerError(String message) {
        return abort(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Aborts the request with 401 Unauthorized status.
     *
     * @param message The error message to send
     * @throws JoltHttpException If writing the response fails
     */
    public JoltContext abortUnauthorized(String message) {
        return abort(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Aborts the request with 403 Forbidden status.
     *
     * @param message The error message to send
     * @throws JoltHttpException If writing the response fails
     */
    public JoltContext abortForbidden(String message) {
        return abort(HttpStatus.FORBIDDEN, message);
    }

    /**
     * Aborts the request with HTTP status 409 Conflict.
     *
     * @param message the error message to send
     */
    public JoltContext abortConflict(String message) {
        return abort(HttpStatus.CONFLICT, message);
    }

    /**
     * Aborts the request with HTTP status 422 Unprocessable Entity.
     *
     * @param message the error message to send
     */
    public JoltContext abortUnprocessableEntity(String message) {
        return abort(HttpStatus.UNPROCESSABLE_ENTITY, message);
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
        return requestContext.getCookie(name);
    }

    /**
     * Retrieves the value of a given cookie as an Optional.
     * 
     * @param name The cookie name to look up.
     * @return The value of the cookie, or an empty Optional if not found.
     */
    public Optional<String> getCookieValue(String name) {
        return getCookie(name) != null && getCookie(name).getValue() != null
                ? Optional.of(getCookie(name).getValue())
                : Optional.empty();
    }

    /**
     * Retrieves the value of a given cookie as a String and process it with
     * optional actions.
     * 
     * @param name       The cookie name to look up.
     * @param onFound    The action to perform if the cookie is found.
     * @param onNotFound The action to perform if the cookie is not found.
     */
    public void getCookieValue(String name, Consumer<String> onFound, Runnable onNotFound) {
        Cookie cookie = getCookie(name);
        if (cookie != null && cookie.getValue() != null) {
            onFound.accept(cookie.getValue());
        } else {
            onNotFound.run();
        }
    }

    /**
     * Returns a {@link CookieBuilder} preconfigured to attach cookies to this
     * response.
     * <p>
     * Example:
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
        return responseContext.addCookie();
    }

    /**
     * Returns a list of all cookies present in the request.
     *
     * @return
     *         A list of cookies, or an empty list if none are present.
     */
    public List<Cookie> getCookies() {
        return requestContext.getCookies();
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
    public JoltContext removeCookie(String name) {
        responseContext.removeCookie(name);
        return this;
    }

    /**
     * Builds a new {@link Form} by aggregating data from all available sources:
     * <ul>
     * <li>Query parameters (or form-encoded parameters)</li>
     * <li>JSON request body (if the Content-Type is "application/json")</li>
     * <li>Path parameters extracted from the URL</li>
     * </ul>
     * In case of duplicate keys, the values from later sources override those from
     * earlier sources.
     *
     * @param excludes The names of the fields to exclude from the form.
     * @return A new {@link Form} instance populated with all available input data.
     * @throws JoltBadRequestException If JSON parsing fails.
     */
    public Form buildForm(String... excludes) {
        Map<String, String> formData = new HashMap<>();
        addQueryParameters(formData, excludes);
        addJsonBodyParameters(formData, excludes);

        Map<String, String> sanitizedPathParams = new HashMap<>();
        pathParams.forEach((key, value) -> {
            sanitizedPathParams.put(key, InputSanitizer.sanitizeString(value));
        });
        formData.putAll(sanitizedPathParams);

        return new Form(formData);
    }

    /**
     * <strong>WARNING!</strong> This method doesn't need to be call, but won't
     * result in any errors if it is.
     * 
     * @return The commited JoltContext response.
     */
    public JoltContext commit() {
        if (committed) {
            return this;
        }
        responseContext.commit();
        committed = true;
        return this;
    }

    /**
     * Adds query or form-encoded parameters from the HTTP request to the provided
     * form data map.
     *
     * @param formData The map to which query parameters will be added.
     * @param excludes The fields to exclude.
     */
    private void addQueryParameters(Map<String, String> formData, String... excludes) {
        requestContext.getRequest().getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0 && !shouldExclude(key, excludes)) {
                formData.put(key, InputSanitizer.sanitizeString(values[0]));
            }
        });
    }

    /**
     * Parses the JSON request body (if applicable) and adds its key-value pairs to
     * the provided form data map.
     *
     * @param formData The map to which JSON parameters will be added.
     * @param excludes The fields to exclude.
     * @throws JoltBadRequestException If the JSON body cannot be parsed.
     */
    private void addJsonBodyParameters(Map<String, String> formData, String... excludes) {
        String contentType = requestContext.getRequest().getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            String raw = bodyRaw();
            if (!raw.isEmpty()) {
                try {
                    Map<String, Object> parsed = JacksonUtil.getObjectMapper().readValue(raw,
                            new TypeReference<Map<String, Object>>() {
                            });
                    parsed.forEach((key, object) -> {
                        if (!shouldExclude(key, excludes)) {
                            String valueStr = (object == null) ? "" : object.toString();
                            formData.put(key, InputSanitizer.sanitizeJsonString(valueStr));
                        }
                    });
                } catch (IOException e) {
                    throw new JoltBadRequestException("Failed to parse JSON body : " + e.getMessage());
                }
            }
        }
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