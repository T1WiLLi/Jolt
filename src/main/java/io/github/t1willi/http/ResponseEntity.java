package io.github.t1willi.http;

import lombok.Getter;

import java.util.*;

import io.github.t1willi.template.JoltModel;

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

    private ResponseEntity(HttpStatus status,
            Map<String, List<String>> headers,
            T body,
            boolean redirect) {
        this.status = status;
        this.headers = headers != null ? headers : new LinkedHashMap<>();
        this.body = body;
        this.redirect = redirect;
    }

    /** Full constructor (all four fields). */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body,
            boolean redirect,
            Map<String, List<String>> headers) {
        return new ResponseEntity<>(status, headers, body, redirect);
    }

    /** status+body, no redirect, no headers. */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body) {
        return new ResponseEntity<>(status, new LinkedHashMap<>(), body, false);
    }

    /** status only, no body, no redirect, no headers. */
    public static ResponseEntity<Void> of(
            HttpStatus status) {
        return new ResponseEntity<>(status, new LinkedHashMap<>(), null, false);
    }

    /** status+body+redirect, no headers. */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body,
            boolean redirect) {
        return new ResponseEntity<>(status, new LinkedHashMap<>(), body, redirect);
    }

    /** status+body, plus single-value headers. */
    public static <U> ResponseEntity<U> of(
            HttpStatus status,
            U body,
            Map<String, String> singleValueHeaders) {
        var multi = new LinkedHashMap<String, List<String>>();
        singleValueHeaders.forEach((k, v) -> multi.put(k, List.of(v)));
        return new ResponseEntity<>(status, multi, body, false);
    }

    public static <U> ResponseEntity<U> ok(U body) {
        return of(HttpStatus.OK, body);
    }

    public static ResponseEntity<Void> noContent() {
        return of(HttpStatus.NO_CONTENT);
    }

    public static <U> ResponseEntity<U> created(U body, String location) {
        return of(HttpStatus.CREATED, body, Map.of("Location", location));
    }

    public static ResponseEntity<Void> notFound(String message) {
        return of(HttpStatus.NOT_FOUND, null, Map.of("X-Error-Message", message));
    }

    /** 302 Found + Location header, no body. */
    public static ResponseEntity<Void> redirect(String location) {
        return of(HttpStatus.FOUND, null, true, Map.of("Location", List.of(location)));
    }

    /**
     * 302 Found + Location header + carry a JoltModel.
     * If the target is a template we’ll pick it up; otherwise it’s ignored.
     */
    public static ResponseEntity<JoltModel> redirect(
            String location,
            JoltModel model) {
        return new ResponseEntity<>(
                HttpStatus.FOUND,
                Map.of("Location", List.of(location)),
                model,
                true);
    }

    public ResponseEntity<T> header(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    public ResponseEntity<T> status(HttpStatus newStatus) {
        return new ResponseEntity<>(
                newStatus,
                new LinkedHashMap<>(this.headers),
                this.body,
                this.redirect);
    }

    public <U> ResponseEntity<U> body(U newBody) {
        return new ResponseEntity<>(
                this.status,
                new LinkedHashMap<>(this.headers),
                newBody,
                this.redirect);
    }
}