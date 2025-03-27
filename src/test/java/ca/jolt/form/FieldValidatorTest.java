package ca.jolt.form;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.t1willi.form.FieldValidator;
import io.github.t1willi.form.Form;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FieldValidator}.
 */
class FieldValidatorTest {

    private Form mockForm;
    private FieldValidator fieldValidator;

    @BeforeEach
    void setUp() {
        // Mock the Form object
        mockForm = mock(Form.class);

        // Create a test FieldValidator instance
        fieldValidator = new FieldValidator("testField", mockForm);

        // Set up a default value for the field in the mock form
        when(mockForm.getValue("testField")).thenReturn("testValue");
    }

    @Test
    void testRequiredFieldValidation_PassesWhenNotEmpty() {
        when(mockForm.getValue("testField")).thenReturn("Some Value");

        fieldValidator.required();
        boolean result = fieldValidator.verify();

        assertTrue(result);
        verify(mockForm, never()).addError(anyString(), anyString());
    }

    @Test
    void testMinLengthValidation() {
        when(mockForm.getValue("testField")).thenReturn("abc");

        fieldValidator.minLength(3);
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("ab");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Value must be at least 3 characters long.");
    }

    @Test
    void testMaxLengthValidation() {
        when(mockForm.getValue("testField")).thenReturn("abcdefgh");

        fieldValidator.maxLength(10);
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("abcdefghijk");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Value must be at most 10 characters long.");
    }

    @Test
    void testEmailValidation() {
        when(mockForm.getValue("testField")).thenReturn("valid@example.com");

        fieldValidator.email();
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("invalid-email");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Invalid email format.");
    }

    @Test
    void testTrimTransformation() {
        when(mockForm.getValue("testField")).thenReturn("   spaced   ");

        fieldValidator.trim();
        fieldValidator.verify();

        verify(mockForm).setValue("testField", "spaced");
    }

    @Test
    void testLowerCaseTransformation() {
        when(mockForm.getValue("testField")).thenReturn("MiXeDcAsE");

        fieldValidator.toLowerCase();
        fieldValidator.verify();

        verify(mockForm).setValue("testField", "mixedcase");
    }

    @Test
    void testUpperCaseTransformation() {
        when(mockForm.getValue("testField")).thenReturn("MiXeDcAsE");

        fieldValidator.toUpperCase();
        fieldValidator.verify();

        verify(mockForm).setValue("testField", "MIXEDCASE");
    }

    @Test
    void testNumericValidation_Min() {
        when(mockForm.getValue("testField")).thenReturn("10");

        fieldValidator.min(5);
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("3");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Value must be at least 5.");
    }

    @Test
    void testNumericValidation_Max() {
        when(mockForm.getValue("testField")).thenReturn("7");

        fieldValidator.max(10);
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("15");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Value must be at most 10.");
    }

    @Test
    void testStrongPasswordValidation() {
        when(mockForm.getValue("testField")).thenReturn("Aa1@valid");

        fieldValidator.strongPassword();
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("weakpass");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Password must be strong.");
    }

    @Test
    void testIPAddressValidation() {
        when(mockForm.getValue("testField")).thenReturn("192.168.1.1");

        fieldValidator.ipAddress();
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("999.999.999.999");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "Invalid IP address format.");
    }

    @Test
    void testClampValidation() {
        when(mockForm.getValue("testField")).thenReturn("8");

        fieldValidator.clamp(5, 10);
        assertTrue(fieldValidator.verify());

        when(mockForm.getValue("testField")).thenReturn("3");
        assertFalse(fieldValidator.verify());
        verify(mockForm).addError("testField", "testField");

        when(mockForm.getValue("testField")).thenReturn("12");
        assertFalse(fieldValidator.verify());
        verify(mockForm, times(2)).addError("testField", "testField");
    }
}