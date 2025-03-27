package io.github.t1willi.routing.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.exceptions.JoltBadRequestException;
import io.github.t1willi.files.JoltFile;
import io.github.t1willi.utils.JacksonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.Getter;

/**
 * Represents the request context, providing utility methods to access
 * various details about the HTTP request.
 */
final class RequestContext {

    @Getter
    private final HttpServletRequest request;

    /**
     * Constructs a new RequestContext instance.
     *
     * @param request The {@link HttpServletRequest} instance.
     */
    public RequestContext(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the HTTP method of the request.
     *
     * @return The request method (e.g., GET, POST).
     */
    public String method() {
        return request.getMethod();
    }

    /**
     * Retrieves the request path, ensuring a valid format.
     *
     * @return The request path.
     */
    public String getPath() {
        String p = (request.getPathInfo() != null) ? request.getPathInfo() : request.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    /**
     * Extracts the client's IP address, considering common proxy headers.
     *
     * @return The client's IP address.
     */
    public String clientIp() {
        String ip = getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * Retrieves the user-agent string from the request headers.
     *
     * @return The user-agent string, or null if not present.
     */
    public String userAgent() {
        return getHeader("User-Agent");
    }

    /**
     * Retrieves a specific request header value.
     *
     * @param name The name of the header.
     * @return The header value, or null if not present.
     */
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    /**
     * Retrieves a query parameter by name.
     *
     * @param name The parameter name.
     * @return The parameter value, or null if not found.
     */
    public String getParameter(String name) {
        return request.getParameter(name);
    }

    /**
     * Retrieves all query parameters as a map.
     *
     * @return A map of query parameters and their values.
     */
    public Map<String, List<String>> getAllQuery() {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.of(entry.getValue())));
    }

    /**
     * Extracts the bearer token from the Authorization header.
     *
     * @return An {@link Optional} containing the token if present.
     */
    public Optional<String> bearerToken() {
        String authHeader = getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return Optional.of(authHeader.substring(7).trim());
        }
        return Optional.empty();
    }

    /**
     * Reads the raw request body as a string.
     *
     * @return The request body.
     * @throws JoltBadRequestException If an error occurs while reading.
     */
    public String bodyRaw() {
        try {
            request.setCharacterEncoding("UTF-8");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to read request body: " + e.getMessage());
        }
    }

    /**
     * Parses the request body as a JSON object of the specified type.
     *
     * @param <T>  The expected type.
     * @param type The class of the type.
     * @return The parsed object or null if the body is empty.
     */
    public <T> T body(Class<T> type) {
        try {
            String raw = bodyRaw();
            if (raw.isEmpty()) {
                return null;
            }
            return JacksonUtil.getObjectMapper().readValue(raw, type);
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
        }
    }

    /**
     * Parses the request body as a JSON object using a {@link TypeReference}.
     *
     * @param <T>     The expected type.
     * @param typeRef The type reference for deserialization.
     * @return The parsed object or null if the body is empty.
     */
    public <T> T body(TypeReference<T> typeRef) {
        String raw = bodyRaw();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return JacksonUtil.getObjectMapper().readValue(raw, typeRef);
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
        }
    }

    /**
     * Retrieves uploaded files from the request.
     *
     * @return A list of uploaded files.
     */
    public List<JoltFile> getFiles() {
        List<JoltFile> files = new ArrayList<>();
        try {
            Collection<Part> parts = request.getParts();
            if (parts != null) {
                for (Part part : parts) {
                    String submittedFileName = part.getSubmittedFileName();
                    if (submittedFileName != null && !submittedFileName.trim().isEmpty()) {
                        byte[] data = part.getInputStream().readAllBytes();
                        if (data.length > 0) {
                            files.add(new JoltFile(submittedFileName, part.getContentType(), data.length, data));
                        }
                    }
                }
            }
        } catch (ServletException | IOException e) {
            throw new JoltBadRequestException("Failed to retrieve uploaded files: " + e.getMessage());
        }
        return files;
    }

    /**
     * Retrive a cookie by name.
     * 
     * @param name The name of the cookie.
     * @return The cookie.
     */
    public Cookie getCookie(String name) {
        return request.getCookies() == null ? null
                : java.util.Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals(name))
                        .findFirst().orElse(null);
    }

    /**
     * Retrieve all cookies.
     * 
     * @return A list of cookies.
     */
    public List<Cookie> getCookies() {
        return request.getCookies() != null ? Arrays.asList(request.getCookies()) : Collections.emptyList();
    }
}
