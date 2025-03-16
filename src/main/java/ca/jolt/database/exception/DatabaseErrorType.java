package ca.jolt.database.exception;

import ca.jolt.http.HttpStatus;
import lombok.Getter;

/**
 * Categorization of database errors to provide appropriate HTTP status codes
 * and handling.
 */
public enum DatabaseErrorType {
    CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE),
    QUERY_SYNTAX_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST),
    DUPLICATE_KEY(HttpStatus.CONFLICT),
    DATA_INTEGRITY_ERROR(HttpStatus.BAD_REQUEST),
    TRANSACTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    MAPPING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    @Getter
    private final HttpStatus status;

    DatabaseErrorType(HttpStatus status) {
        this.status = status;
    }
}
