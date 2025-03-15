package ca.jolt.routing.context;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.exceptions.JoltBadRequestException;
import ca.jolt.files.JoltFile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.Getter;

final class RequestContext {

    @Getter
    private final HttpServletRequest request;

    public RequestContext(HttpServletRequest request) {
        this.request = request;
    }

    public String method() {
        return request.getMethod();
    }

    public String getPath() {
        String p = (request.getPathInfo() != null) ? request.getPathInfo() : request.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

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

    public String userAgent() {
        return getHeader("User-Agent");
    }

    public String getHeader(String n) {
        return request.getHeader(n);
    }

    public String getParameter(String n) {
        return request.getParameter(n);
    }

    public Map<String, List<String>> getAllQuery() {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.of(entry.getValue())));
    }

    public Optional<String> bearerToken() {
        String authHeader = getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return Optional.of(authHeader.substring(7).trim());
        }
        return Optional.empty();
    }

    public String bodyRaw() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to read request body: " + e.getMessage());
        }
        return sb.toString().trim();
    }

    public <T> T body(Class<T> type) {
        try {
            String raw = bodyRaw();
            if (raw.isEmpty()) {
                return null;
            }
            return new ObjectMapper().readValue(raw, type);
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
        }
    }

    public <T> T body(TypeReference<T> typeRef) {
        String raw = bodyRaw();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(raw, typeRef);
        } catch (IOException e) {
            throw new JoltBadRequestException("Failed to parse JSON body: " + e.getMessage());
        }
    }

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
                            files.add(new JoltFile(
                                    submittedFileName,
                                    part.getContentType(),
                                    data.length,
                                    data));
                        }
                    }
                }
            }
        } catch (ServletException | IOException e) {
            throw new JoltBadRequestException("Failed to retrieve uploaded files: " + e.getMessage());
        }
        return files;
    }

    public Cookie getCookie(String name) {
        return request.getCookies() == null ? null
                : java.util.Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals(name))
                        .findFirst().orElse(null);
    }

    public List<Cookie> getCookies() {
        return request.getCookies() != null ? Arrays.asList(request.getCookies()) : Collections.emptyList();
    }
}
