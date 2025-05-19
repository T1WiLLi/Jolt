package io.github.t1willi.http;

import lombok.Getter;
import java.util.*;

/**
 * A wrapper for HTTP status, headers, body—and a redirect flag—
 * exposed entirely through static factories.
 */
@Getter
public class ResponseEntity<T> {
    private final HttpStatus status;
    private final Map<String, List<String>> headers;
    private final T body;
    private final boolean redirect;

    private ResponseEntity(HttpStatus status, Map<String, List<String>> headers, T body, boolean redirect) {
        this.status = status;
        this.headers = headers;
        this.body = body;
        this.redirect = redirect;
    }

    public static <U> ResponseEntity<U> of(HttpStatus status, U body, boolean redirect,
            Map<String, List<String>> headers) {
        return new ResponseEntity<>(status, headers, body, redirect);
    }

    public static <U> ResponseEntity<U> ok(U body) {
        return of(HttpStatus.OK, body, false, new LinkedHashMap<>());
    }

    public static <U> ResponseEntity<U> created(U body, String location) {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("Location", List.of(location));
        return of(HttpStatus.CREATED, body, false, h);
    }

    public static ResponseEntity<Void> noContent() {
        return of(HttpStatus.NO_CONTENT, null, false, new LinkedHashMap<>());
    }

    public static <U> ResponseEntity<U> notFound(String message) {
        var h = new LinkedHashMap<String, List<String>>();
        h.put("X-Error-Message", List.of(message));
        return of(HttpStatus.NOT_FOUND, null, false, h);
    }

    public static ResponseEntity<Void> redirect(String location) {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("Location", List.of(location));
        return of(HttpStatus.FOUND, null, true, h);
    }

    public ResponseEntity<T> header(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    public ResponseEntity<T> status(HttpStatus newStatus) {
        return new ResponseEntity<>(newStatus, new LinkedHashMap<>(this.headers), this.body, this.redirect);
    }

    public <U> ResponseEntity<U> body(U newBody) {
        return new ResponseEntity<>(this.status, new LinkedHashMap<>(this.headers), newBody, this.redirect);
    }
}