package io.github.t1willi.form;

import java.util.function.Predicate;

public interface Rule {

    boolean validate(String data);

    String getErrorMessage();

    static Rule custom(Predicate<String> predicate, String errorMessage) {
        return new Rule() {
            @Override
            public boolean validate(String data) {
                return predicate.test(data);
            }

            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        };
    }
}
