package ca.jolt.routing.context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import ca.jolt.cookie.CookieBuilder;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.files.JoltFile;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.MimeInterpreter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

final class ResponseContext {

    @Getter
    private final HttpServletResponse response;
    private final ResponseBuffer buffer = new ResponseBuffer();

    public ResponseContext(HttpServletResponse response) {
        this.response = response;
    }

    public void setStatus(HttpStatus status) {
        buffer.setStatus(status);
    }

    public void setStatus(int code) {
        buffer.setStatus(HttpStatus.fromCode(code));
    }

    public HttpStatus getStatus() {
        return buffer.status;
    }

    public void setHeader(String name, String value) {
        buffer.setHeader(name, value);
    }

    public void setContentType(String type) {
        buffer.setContentType(type);
    }

    public void redirect(String location) {
        setStatus(HttpStatus.FOUND);
        setHeader("Location", location);
    }

    public void text(String data) {
        setContentType("text/plain;charset=UTF-8");
        buffer.setTextBody(data);
    }

    public void json(Object data) {
        setContentType("application/json;charset=UTF-8");
        buffer.setJsonBody(data);
    }

    public void html(String html) {
        setContentType("text/html;charset=UTF-8");
        buffer.setTextBody(html);
    }

    public void write(Object data) throws IOException {
        response.getWriter().write(data.toString());
    }

    public void serve(String resource) {
        String normalizedResource = resource.startsWith("/") ? resource.substring(1) : resource;
        InputStream in = getClass().getClassLoader().getResourceAsStream("static/" + normalizedResource);
        if (in == null) {
            throw new JoltHttpException(HttpStatus.NOT_FOUND, "Static resource not found: " + resource);
        }
        try {
            byte[] data = in.readAllBytes();
            int dotIndex = resource.lastIndexOf('.');
            String extension = (dotIndex != -1) ? resource.substring(dotIndex) : "";
            String mimeType = MimeInterpreter.getMime(extension);
            setHeader("Content-Type", mimeType);
            buffer.setBinaryBody(data);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error serving static resource: " + e.getMessage(), e);
        }
    }

    public void download(JoltFile file, String filename) {
        setHeader("Content-Type", file.getContentType());
        setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        buffer.setBinaryBody(file.getData());
    }

    public CookieBuilder addCookie() {
        return new CookieBuilder(response);
    }

    public void removeCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public void commit() {
        response.setStatus(buffer.status.code());

        for (Map.Entry<String, String> header : buffer.headers.entrySet()) {
            response.setHeader(header.getKey(), header.getValue());
        }

        if (buffer.contentType != null) {
            response.setContentType(buffer.contentType);
        }

        try {
            if (buffer.body != null) {
                if (buffer.isJsonBody) {
                    new ObjectMapper().registerModule(new Jdk8Module()).writeValue(response.getWriter(), buffer.body);
                } else {
                    response.getWriter().write((String) buffer.body);
                }
            } else if (buffer.isBinaryBody && buffer.binaryData != null) {
                response.getOutputStream().write(buffer.binaryData);
            }
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error writing response: " + e.getMessage(), e);
        }
    }

    private static class ResponseBuffer {
        private HttpStatus status = HttpStatus.OK;
        private Map<String, String> headers = new HashMap<>();
        private String contentType = null;
        private Object body = null;
        private boolean isJsonBody = false;
        private boolean isBinaryBody = false;
        private byte[] binaryData = null;

        public void setStatus(HttpStatus status) {
            this.status = status;
        }

        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setTextBody(String body) {
            this.body = body;
            this.isJsonBody = false;
            this.isBinaryBody = false;
        }

        public void setJsonBody(Object body) {
            this.body = body;
            this.isJsonBody = true;
            this.isBinaryBody = false;
        }

        public void setBinaryBody(byte[] binaryData) {
            this.binaryData = binaryData;
            this.isBinaryBody = true;
            this.isJsonBody = false;
        }
    }
}
