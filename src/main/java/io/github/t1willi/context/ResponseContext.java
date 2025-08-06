package io.github.t1willi.context;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import io.github.t1willi.cookie.CookieBuilder;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.files.JoltFile;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.utils.JacksonUtil;
import io.github.t1willi.utils.MimeInterpreter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

/**
 * Handles the HTTP response context, allowing modification of response
 * attributes such as status, headers, body, and cookies.
 * Provides methods to send text, JSON, HTML, and binary responses, as well as
 * handle redirects and file downloads.
 */
final class ResponseContext {

    /** The underlying HttpServletResponse object. */
    @Getter
    private final HttpServletResponse response;
    private final ResponseBuffer buffer = new ResponseBuffer();

    /**
     * Constructs a ResponseContext with the given HttpServletResponse.
     *
     * @param response the HttpServletResponse to wrap
     */
    public ResponseContext(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * Sets the HTTP status of the response.
     *
     * @param status the HttpStatus to set
     */
    public void setStatus(HttpStatus status) {
        buffer.setStatus(status);
    }

    /**
     * Sets the HTTP status of the response using a numeric code.
     *
     * @param code the HTTP status code
     */
    public void setStatus(int code) {
        buffer.setStatus(HttpStatus.fromCode(code));
    }

    /**
     * Retrieves the current HTTP status of the response.
     *
     * @return the HttpStatus of the response
     */
    public HttpStatus getStatus() {
        return buffer.status;
    }

    /**
     * Sets a header in the response.
     *
     * @param name  the name of the header
     * @param value the value of the header
     */
    public void setHeader(String name, String value) {
        buffer.setHeader(name, value);
    }

    /**
     * Sets the content type of the response.
     *
     * @param type the MIME type of the response content
     */
    public void setContentType(String type) {
        buffer.setContentType(type);
    }

    /**
     * Sends a plain text response.
     *
     * @param data the text to send in the response
     */
    public void text(String data) {
        setContentType("text/plain;charset=UTF-8");
        buffer.setTextBody(data);
    }

    public void sendRedirect(String location, int sc) {
        try {
            response.sendRedirect(location, sc);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during redirect: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Sends a JSON response.
     *
     * @param data the object to serialize to JSON and send
     */
    public void json(Object data) {
        setContentType("application/json;charset=UTF-8");
        buffer.setJsonBody(data);
    }

    /**
     * Sends an HTML response.
     *
     * @param html the HTML content to send
     */
    public void html(String html) {
        setContentType("text/html;charset=UTF-8");
        buffer.setTextBody(html);
    }

    /**
     * Writes raw data to the response output stream.
     *
     * @param data the data to write
     * @throws IOException if an I/O error occurs
     */
    public void write(Object data) throws IOException {
        response.getWriter().write(data.toString());
    }

    /**
     * Serves a static resource from the "static" directory.
     *
     * @param resource the resource path relative to the "static" directory
     */
    public void serve(HttpServletRequest request,
            HttpServletResponse response,
            String resource) {
        String normalized = resource.startsWith("/")
                ? resource.substring(1)
                : resource;
        String target = "/static/" + normalized;
        ServletContext sc = request.getServletContext();

        try {
            if (sc.getResource(target) == null) {
                throw new JoltHttpException(
                        HttpStatus.NOT_FOUND,
                        "Resource not found: " + resource);
            }

            String ext = normalized.contains(".")
                    ? normalized.substring(normalized.lastIndexOf('.') + 1)
                    : "";
            String mime = sc.getMimeType(normalized);
            if (mime == null) {
                mime = MimeInterpreter.getMime(ext);
            }
            response.setContentType(mime);

            request.getRequestDispatcher(target)
                    .forward(request, response);

        } catch (MalformedURLException e) {
            throw new JoltHttpException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid resource URL: " + e.getMessage(),
                    e);
        } catch (ServletException | IOException e) {
            throw new JoltHttpException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error serving static resource: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Triggers a file download.
     *
     * @param file     the file to send
     * @param filename the name of the downloaded file
     */
    public void download(JoltFile file, String filename) {
        setHeader("Content-Type", file.getContentType());
        setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        buffer.setBinaryBody(file.getData());
    }

    /**
     * Adds a new cookie to the response.
     *
     * @return a CookieBuilder instance for constructing cookies
     */
    public CookieBuilder addCookie() {
        return new CookieBuilder(response);
    }

    /**
     * Removes a cookie from the response.
     *
     * @param name the name of the cookie to remove
     */
    public void removeCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Commits the buffered response to the client, sending headers and body
     * content.
     */
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
                    JacksonUtil.getObjectMapper().writeValue(response.getWriter(), buffer.body);
                } else {
                    response.getWriter().write((String) buffer.body);
                }
            } else if (buffer.isBinaryBody && buffer.binaryData != null) {
                response.getOutputStream().write(buffer.binaryData);
            }
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing response: " + e.getMessage(),
                    e);
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
