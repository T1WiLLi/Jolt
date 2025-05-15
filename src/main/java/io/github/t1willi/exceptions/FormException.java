package io.github.t1willi.exceptions;

import io.github.t1willi.form.Form;

/**
 * Exception thrown when a form fails validation.
 */
public class FormException extends RuntimeException {
    /**
     * Constructs a new FormException with the invalid form.
     *
     * @param form The form that failed validation.
     */
    public FormException(Form form) {
        super("Form validation failed: " + form.toString());
    }
}