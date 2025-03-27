package io.github.t1willi.database.exception;

import io.github.t1willi.exceptions.JoltHttpException;
import lombok.Getter;

/**
 * Exception thrown for database-related errors.
 * Provides categorization of database exceptions and user-safe error messages.
 */
public class DatabaseException extends JoltHttpException {

    @Getter
    private final DatabaseErrorType errorType;
    @Getter
    private final String technicalDetails;

    public DatabaseException(DatabaseErrorType errorType, String message, String technicalDetails, Throwable cause) {
        super(errorType.getStatus(), message, cause);
        this.errorType = errorType;
        this.technicalDetails = technicalDetails;
    }
}