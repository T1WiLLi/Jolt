package ca.jolt.form;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A specialized {@link Rule} that supports asynchronous validation via
 * {@link CompletableFuture}. This is useful for checks that require
 * non-blocking operations, such as calling remote services or
 * performing database queries.
 *
 * <p>
 * Unlike regular rules, the actual validation occurs in
 * {@link #validateAsync(String, Map)}. The synchronous
 * {@link #validate(String, Map)} method always returns {@code true},
 * effectively doing nothing. The {@link Form#verifyAsync()} method
 * is responsible for invoking {@code validateAsync} on all
 * {@code AsyncRule} instances.
 *
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 * AsyncRule rule = new AsyncRule(
 *         value -> {
 *             // Pretend this is a remote call
 *             return someRemoteService.checkValueAsync(value);
 *         },
 *         "Value was rejected by remote service.");
 * }</pre>
 *
 * @see Rule
 * @see Form#verifyAsync()
 * @see FieldValidator#asyncRule(Function, String)
 * @author William Beaudin
 * @since 1.0
 */
public class AsyncRule implements Rule {

    /**
     * A function that takes the field value and returns a {@link CompletableFuture}
     * that resolves to {@code true} if valid, or {@code false} if invalid.
     */
    private final Function<String, CompletableFuture<Boolean>> asyncValidator;

    /**
     * An error message returned when validation fails.
     */
    private final String errorMessage;

    /**
     * Constructs a new {@code AsyncRule} with the provided async validator function
     * and error message.
     *
     * @param asyncValidator A function that takes a string value and returns a
     *                       {@link CompletableFuture<Boolean>} indicating if the
     *                       value is valid.
     * @param errorMessage   The error message displayed if validation fails.
     */
    public AsyncRule(Function<String, CompletableFuture<Boolean>> asyncValidator, String errorMessage) {
        this.asyncValidator = asyncValidator;
        this.errorMessage = errorMessage;
    }

    /**
     * A no-op for synchronous checks, always returning {@code true}.
     * Asynchronous validation is delegated to {@link #validateAsync(String, Map)}.
     *
     * @param data      The field value to validate (unused).
     * @param allValues All form field values (unused).
     * @return Always {@code true}.
     */
    @Override
    public boolean validate(String data) {
        return true;
    }

    /**
     * Performs the actual asynchronous validation for this rule.
     *
     * @param data      The field value to validate.
     * @param allValues All form field values, which may be useful if cross-field
     *                  checks are needed.
     * @return A {@link CompletableFuture} that resolves to {@code true} if valid,
     *         or {@code false} otherwise.
     */
    public CompletableFuture<Boolean> validateAsync(String data) {
        return asyncValidator.apply(data);
    }

    /**
     * Returns the error message for this rule, used if {@code validateAsync}
     * completes
     * with {@code false}.
     *
     * @return The error message.
     */
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}