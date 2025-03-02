package ca.jolt.exceptions;

import ca.jolt.form.Form;

/**
 * Thrown when a form fails validation.
 */
public class FormException extends RuntimeException {
    private final transient Form form;

    public FormException(Form form) {
        super("Form validation failed.");
        this.form = form;
    }

    public Form getForm() {
        return form;
    }
}
