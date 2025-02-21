package ca.jolt.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.exceptions.JoltBadRequestException;
import ca.jolt.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class JoltHttpContext { // Improve this so that exception actually return somethings to the client, and
                                     // log it !!

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final HttpServletRequest req;
    private final HttpServletResponse res;

    private final Map<String, String> pathParams;

    public JoltHttpContext(HttpServletRequest req, HttpServletResponse res, Matcher pathMatcher,
            List<String> paramNames) {
        this.req = req;
        this.res = res;
        this.pathParams = extractPathParams(pathMatcher, paramNames);
    }

    // Request (Input)

    public String method() {
        return req.getMethod();
    }

    public String requestPath() {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    public JoltHttpContextValue path(String name) {
        return new JoltHttpContextValue(pathParams.get(name));
    }

    public JoltHttpContextValue query(String name) {
        return new JoltHttpContextValue(req.getParameter(name));
    }

    public Map<String, List<String>> query() {
        Map<String, List<String>> map = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> {
            map.put(key, Arrays.asList(values));
        });
        return map;
    }

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

    public String header(String n) {
        return req.getHeader(n);
    }

    // Response (Output)

    public JoltHttpContext status(HttpStatus status) {
        res.setStatus(status.code());
        return this;
    }

    public JoltHttpContext status(int code) {
        res.setStatus(code);
        return this;
    }

    public JoltHttpContext header(String name, String value) {
        res.setHeader(name, value);
        return this;
    }

    public JoltHttpContext text(String data) {
        try {
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public JoltHttpContext json(String json) {
        try {
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public JoltHttpContext json(Object data) {
        try {
            res.setContentType("application/json;charset=UTF-8");
            JSON_MAPPER.writeValue(res.getWriter(), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public JoltHttpContext html(String html) {
        try {
            res.setContentType("text/html;charset=UTF-8");
            res.getWriter().write(html);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

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