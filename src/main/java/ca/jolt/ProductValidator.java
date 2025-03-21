package ca.jolt;

import ca.jolt.form.Form;

public class ProductValidator {

    public static Form validate(Form form) {
        form.field("name")
                .required()
                .minLength(3)
                .maxLength(50);
        form.field("price")
                .required()
                .min(0)
                .max(9999);
        return form;
    }
}
