package io.github.t1willi.form;

import java.util.List;
import java.util.Map;

public interface Form {
    Field field(String name);

    boolean validate();

    Map<String, String> errors();

    Map<String, List<String>> allErrors();

    Form setValue(String name, String val);
}
