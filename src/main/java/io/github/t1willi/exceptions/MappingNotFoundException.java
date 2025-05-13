package io.github.t1willi.exceptions;

public class MappingNotFoundException extends RuntimeException {
    public MappingNotFoundException(String message) {
        super(message);
    }

    public MappingNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
