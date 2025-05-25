package io.github.t1willi.form;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.template.JoltModel;

/**
 * An interface for handling and processing form data in a web application.
 * <p>
 * The {@code Form} interface defines methods for managing form fields,
 * validating input, handling errors,
 * and building entities or models from form data. It is designed for use in web
 * applications where form
 * submissions need to be processed, validated, and transformed into domain
 * objects or data models for
 * rendering. Implementations of this interface are expected to provide
 * functionality for accessing and
 * manipulating form fields, performing validation, collecting errors, and
 * mapping form data to Java
 * objects or {@link JoltModel} instances. This interface supports both simple
 * and complex form processing
 * scenarios, including type-safe entity creation and field value updates.
 *
 * @since 1.0.0
 */
public interface Form {

    /**
     * Retrieves a form field by its name.
     * <p>
     * This method returns a {@link Field} object representing the form field with
     * the specified name.
     * The field can be used to access or manipulate the field's value, validation
     * rules, or other
     * metadata. This is typically used when processing form submissions to inspect
     * or update individual
     * field data.
     *
     * @param name the name of the form field to retrieve
     * @return the {@link Field} object associated with the specified name
     * @throws IllegalArgumentException if the field name is null or does not exist
     *                                  in the form
     * @since 1.0.0
     */
    Field field(String name);

    /**
     * Validates the form data against defined rules.
     * <p>
     * This method performs validation on all form fields, checking for constraints
     * such as required
     * fields, data formats, or custom validation rules. It returns {@code true} if
     * all fields pass
     * validation, and {@code false} if any validation errors occur. Validation
     * errors can be retrieved
     * using the {@link #errors()} or {@link #allErrors()} methods.
     *
     * @return {@code true} if the form is valid, {@code false} otherwise
     * @since 1.0.0
     */
    boolean validate();

    /**
     * Adds an error message for a specific form field.
     * <p>
     * This method associates an error message with the specified field, typically
     * used when validation
     * fails or when custom error conditions are detected. The error is stored in
     * the form's error
     * collection and can be retrieved later using {@link #errors()} or
     * {@link #allErrors()}. This
     * method returns the form instance to allow method chaining.
     *
     * @param field        the name of the field to which the error applies
     * @param errorMessage the error message describing the validation failure
     * @return this {@link Form} instance for method chaining
     * @throws IllegalArgumentException if the field name or error message is null
     * @since 1.0.0
     */
    Form addError(String field, String errorMessage);

    /**
     * Retrieves a map of field names to their first associated error message.
     * <p>
     * This method returns a map where each key is a field name and the value is the
     * first error message
     * associated with that field. If a field has multiple errors, only the first
     * one is included. This
     * is useful for displaying a single error per field in a user interface.
     *
     * @return a {@link Map} of field names to their first error message, or an
     *         empty map if no errors exist
     * @since 1.0.0
     */
    Map<String, String> errors();

    /**
     * Retrieves a map of field names to all associated error messages.
     * <p>
     * This method returns a map where each key is a field name and the value is a
     * list of all error
     * messages associated with that field. This is useful for displaying all
     * validation errors for each
     * field in a user interface or for detailed error reporting.
     *
     * @return a {@link Map} of field names to lists of error messages, or an empty
     *         map if no errors exist
     * @since 1.0.0
     */
    Map<String, List<String>> allErrors();

    /**
     * Sets the value of a form field.
     * <p>
     * This method updates the value of the specified form field to the provided
     * value. It is typically
     * used to programmatically set or update field values before validation or
     * entity building. The
     * method returns the form instance to allow method chaining.
     *
     * @param name the name of the field to update
     * @param val  the new value for the field
     * @return this {@link Form} instance for method chaining
     * @throws IllegalArgumentException if the field name is null or does not exist
     * @since 1.0.0
     */
    Form setValue(String name, String val);

    /**
     * Builds an entity of the specified type from the form data, ignoring specified
     * fields.
     * <p>
     * This method maps the form data to a Java object of the specified type, using
     * a Jackson
     * {@link TypeReference} to define the target type. Fields listed in
     * {@code ignoreFields} are
     * excluded from the mapping process. This is useful for creating type-safe
     * domain objects from form
     * submissions, such as when processing POST requests in a web application.
     *
     * @param <T>          the type of the entity to build
     * @param type         the {@link TypeReference} specifying the target type
     * @param ignoreFields optional field names to exclude from the mapping process
     * @return the constructed entity of type {@code T}
     * @throws IllegalArgumentException if the type is null or if mapping fails due
     *                                  to invalid data
     * @throws FormMappingException     if an error occurs during the mapping
     *                                  process
     * @since 1.0.0
     */
    <T> T buildEntity(TypeReference<T> type, String... ignoreFields);

    /**
     * Builds an entity of the specified class from the form data, ignoring
     * specified fields.
     * <p>
     * This method maps the form data to a Java object of the specified class,
     * excluding fields listed
     * in {@code ignoreFields} from the mapping process. It is similar to
     * {@link #buildEntity(TypeReference, String...)},
     * but uses a {@link Class} reference for simpler type definitions. This is
     * useful for creating
     * domain objects from form submissions in a type-safe manner.
     *
     * @param <T>          the type of the entity to build
     * @param type         the {@link Class} specifying the target type
     * @param ignoreFields optional field names to exclude from the mapping process
     * @return the constructed entity of type {@code T}
     * @throws IllegalArgumentException if the type is null or if mapping fails due
     *                                  to invalid data
     * @throws FormMappingException     if an error occurs during the mapping
     *                                  process
     * @since 1.0.0
     */
    <T> T buildEntity(Class<T> type, String... ignoreFields);

    /**
     * Updates an existing entity with form data, ignoring specified fields.
     * <p>
     * This method updates the provided entity with values from the form data,
     * excluding fields listed
     * in {@code ignoreFields}. It is typically used to update an existing domain
     * object with new form
     * submission data, such as in a PUT or PATCH request. The updated entity is
     * returned for further
     * processing.
     *
     * @param <T>          the type of the entity to update
     * @param entity       the entity to update with form data
     * @param ignoreFields optional field names to exclude from the update process
     * @return the updated entity of type {@code T}
     * @throws IllegalArgumentException if the entity is null or if mapping fails
     *                                  due to invalid data
     * @throws FormMappingException     if an error occurs during the mapping
     *                                  process
     * @since 1.0.0
     */
    <T> T updateEntity(T entity, String... ignoreFields);

    /**
     * Builds a {@link JoltModel} from the form data, ignoring specified fields.
     * <p>
     * This method creates a {@link JoltModel} containing the form data, excluding
     * fields listed in
     * {@code ignoreFields}. The resulting model can be used for rendering views in
     * an MVC framework
     * or for further processing in a web application. This is useful for preparing
     * data for template
     * rendering after form submission.
     *
     * @param ignoreFields optional field names to exclude from the model
     * @return the constructed {@link JoltModel} containing form data
     * @throws FormMappingException if an error occurs during model construction
     * @since 1.0.0
     */
    JoltModel buildModel(String... ignoreFields);

    /**
     * Returns a string representation of the form.
     * <p>
     * This method provides a human-readable representation of the form, typically
     * including field names,
     * values, and any validation errors. It is useful for debugging or logging form
     * data during
     * development or error handling.
     *
     * @return a string representation of the form
     * @since 1.0.0
     */
    String toString();
}