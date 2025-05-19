package io.github.t1willi.core;

import io.github.t1willi.http.ResponseEntity;

import java.util.Map;

import io.github.t1willi.http.HttpStatus;

public abstract class ApiController extends BaseController {

    protected <T> ResponseEntity<T> okJson(T body) {
        return ResponseEntity.ok(body)
                .header("Content-Type", "application/json");
    }

    protected ResponseEntity<Void> created(String location) {
        return ResponseEntity.created(null, location);
    }

    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent();
    }

    protected ResponseEntity<String> badRequest(String msg) {
        return ResponseEntity
                .of(HttpStatus.BAD_REQUEST, msg, false, Map.of())
                .header("Content-Type", "text/plain");
    }

    protected ResponseEntity<String> notFound(String msg) {
        return ResponseEntity
                .of(HttpStatus.NOT_FOUND, msg, false, Map.of())
                .header("Content-Type", "text/plain");
    }
}
