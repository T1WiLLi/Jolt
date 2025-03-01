package ca.jolt.form;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AsyncRule implements Rule {
    private final Function<String, CompletableFuture<Boolean>> asyncValidator;
    private final String errorMessage;

    public AsyncRule(Function<String, CompletableFuture<Boolean>> asyncValidator, String errorMessage) {
        this.asyncValidator = asyncValidator;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean validate(String data, Map<String, String> allValues) {
        return true;
    }

    public CompletableFuture<Boolean> validateAsync(String data, Map<String, String> allValues) {
        return asyncValidator.apply(data);
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
