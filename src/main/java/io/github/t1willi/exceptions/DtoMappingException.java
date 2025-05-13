package io.github.t1willi.exceptions;

public class DtoMappingException extends RuntimeException {
    public DtoMappingException(String message) {
        super(message);
    }

    public DtoMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
