package io.github.t1willi.form;

public interface Field {
    Field minStringLength(int min, String message);

    Field maxStringLength(int max, String message);

    Field min(int min, String message);

    Field max(int max, String message);

    Field email();

    Field email(String message);
}
