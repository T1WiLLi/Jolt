package ca.jolt.routing.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.exceptions.JoltBadRequestException;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.form.Form;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.builder.CookieBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class JoltHttpContext {

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

    public HttpServletRequest getRequest() {
        return req;
    }

    public String method() {
        return req.getMethod();
    }

    public String requestPath() {
        String p = (req.getPathInfo() != null) ? req.getPathInfo() : req.getServletPath();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    public PathContextValue path(String name) {
        return new PathContextValue(pathParams.get(name));
    }

    public QueryContextValue query(String name) {
        return new QueryContextValue(req.getParameter(name));
    }

    public Map<String, List<String>> query() {
        Map<String, List<String>> map = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> map.put(key, Arrays.asList(values)));
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

    public HttpServletResponse getResponse() {
        return res;
    }

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
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing text response", e);
        }
        return this;
    }

    public JoltHttpContext json(Map<String, Object> json) {
        try {
            res.setContentType("application/json;charset=UTF-8");
            JSON_MAPPER.writeValue(res.getWriter(), json);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing JSON response", e);
        }
        return this;
    }

    public JoltHttpContext json(Object data) {
        try {
            res.setContentType("application/json;charset=UTF-8");
            JSON_MAPPER.writeValue(res.getWriter(), data);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing JSON response", e);
        }
        return this;
    }

    public JoltHttpContext html(String html) {
        try {
            res.setContentType("text/html;charset=UTF-8");
            res.getWriter().write(html);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing HTML response", e);
        }
        return this;
    }

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

    public CookieBuilder addCookie() {
        return new CookieBuilder(this.res);
    }

    public List<Cookie> getCookies() {
        return req.getCookies() != null ? Arrays.asList(req.getCookies()) : Collections.emptyList();
    }

    public JoltHttpContext removeCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
        return this;
    }

    public Form queryToForm(String... excludes) {
        Map<String, String> formData = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> {
            if (!shouldExclude(key, excludes) && values.length > 0) {
                formData.put(key, values[0]);
            }
        });
        return new Form(formData);
    }

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

    private boolean shouldExclude(String key, String[] excludes) {
        for (String exclude : excludes) {
            if (exclude.equals(key)) {
                return true;
            }
        }
        return false;
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