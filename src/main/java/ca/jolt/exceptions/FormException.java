package ca.jolt.exceptions;

import ca.jolt.form.Form;

/**
 * Exception thrown when a form fails validation.
 */
public class FormException extends RuntimeException {
    private final transient Form form;

    /**
     * Constructs a new FormException with the invalid form.
     *
     * @param form The form that failed validation.
     */
    public FormException(Form form) {
        super("Form validation failed.");
        this.form = form;
    }

    /**
     * Returns the form that caused this exception.
     *
     * @return The invalid form.
     */
    public Form getForm() {
        return form;
    }
}