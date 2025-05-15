package io.github.t1willi.form;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive test suite for the form validation framework in
 * io.github.t1willi.form.
 * Tests DefaultForm, DefaultField, and BaseRules, covering field validation,
 * error handling,
 * type conversion, and rule application.
 */
class FormTests {

    @Test
    @DisplayName("Test DefaultForm value setting and retrieval")
    void testFormSetAndGetValues() {
        DefaultForm form = new DefaultForm();
        form.setValue("name", "John Doe").setValue("email", "john@example.com");

        assertEquals("John Doe", form.getValues().get("name"), "Name field should have correct value");
        assertEquals("john@example.com", form.getValues().get("email"), "Email field should have correct value");
        assertNull(form.getValues().get("age"), "Non-existent field should return null");
    }

    @Test
    @DisplayName("Test DefaultForm with initial data")
    void testFormWithInitialData() {
        Map<String, String> initial = Map.of("name", "Jane Doe", "age", "30");
        DefaultForm form = new DefaultForm(initial);

        assertEquals("Jane Doe", form.getValues().get("name"), "Initial name should be set");
        assertEquals("30", form.getValues().get("age"), "Initial age should be set");
    }

    @Test
    @DisplayName("Test empty form validation")
    void testEmptyFormValidation() {
        DefaultForm form = new DefaultForm();
        assertTrue(form.validate(), "Empty form should validate successfully");
        assertTrue(form.errors().isEmpty(), "Empty form should have no errors");
        assertTrue(form.allErrors().isEmpty(), "Empty form should have no error lists");
    }

    @Test
    @DisplayName("Test required field validation")
    void testRequiredField() {
        DefaultForm form = new DefaultForm();
        form.field("name").required("Name is required");

        // Invalid: null value
        assertFalse(form.validate(), "Form with missing required field should fail validation");
        assertEquals("Name is required", form.errors().get("name"), "Should have required error");
        assertEquals(List.of("Name is required"), form.allErrors().get("name"), "Should have required error list");

        // Invalid: empty string
        form.setValue("name", "");
        assertFalse(form.validate(), "Form with empty required field should fail validation");
        assertEquals("Name is required", form.errors().get("name"), "Should have required error");

        // Valid: non-empty string
        form.setValue("name", "John");
        assertTrue(form.validate(), "Form with valid required field should pass validation");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test minLength and maxLength validation")
    void testLengthValidation() {
        DefaultForm form = new DefaultForm();
        form.field("username").min(5, "Username must be at least 5 characters")
                .max(10, "Username must be at most 10 characters");

        // Invalid: too short
        form.setValue("username", "bob");
        assertFalse(form.validate(), "Form with too short username should fail");
        assertEquals("Username must be at least 5 characters", form.errors().get("username"),
                "Should have minLength error");

        // Invalid: too long
        form.setValue("username", "toolongusername");
        assertFalse(form.validate(), "Form with too long username should fail");
        assertEquals("Username must be at most 10 characters", form.errors().get("username"),
                "Should have maxLength error");

        // Valid: correct length
        form.setValue("username", "johnny");
        assertTrue(form.validate(), "Form with valid username length should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test email validation")
    void testEmailValidation() {
        DefaultForm form = new DefaultForm();
        form.field("email").email("Invalid email address");

        // Invalid: incorrect email
        form.setValue("email", "not-an-email");
        assertFalse(form.validate(), "Form with invalid email should fail");
        assertEquals("Invalid email address", form.errors().get("email"), "Should have email error");

        // Valid: correct email
        form.setValue("email", "test@example.com");
        assertTrue(form.validate(), "Form with valid email should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test alphanumeric validation")
    void testAlphanumericValidation() {
        DefaultForm form = new DefaultForm();
        form.field("code").alphanumeric("Only letters and digits allowed");

        // Invalid: contains special characters
        form.setValue("code", "abc#123");
        assertFalse(form.validate(), "Form with non-alphanumeric code should fail");
        assertEquals("Only letters and digits allowed", form.errors().get("code"), "Should have alphanumeric error");

        // Valid: alphanumeric only
        form.setValue("code", "abc123");
        assertTrue(form.validate(), "Form with valid alphanumeric code should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test phone number validation")
    void testPhoneNumberValidation() {
        DefaultForm form = new DefaultForm();
        form.field("phone").phoneNumber("Invalid phone number");

        // Invalid: incorrect format
        form.setValue("phone", "123");
        assertFalse(form.validate(), "Form with invalid phone number should fail");
        assertEquals("Invalid phone number", form.errors().get("phone"), "Should have phone number error");

        // Valid: correct format
        form.setValue("phone", "+1-555-123-4567");
        assertTrue(form.validate(), "Form with valid phone number should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test zip code validation")
    void testZipCodeValidation() {
        DefaultForm form = new DefaultForm();
        form.field("zip").zipCode("Invalid ZIP code");

        // Invalid: incorrect format
        form.setValue("zip", "123");
        assertFalse(form.validate(), "Form with invalid ZIP code should fail");
        assertEquals("Invalid ZIP code", form.errors().get("zip"), "Should have ZIP code error");

        // Valid: correct format
        form.setValue("zip", "12345");
        assertTrue(form.validate(), "Form with valid ZIP code should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");

        // Valid: extended format
        form.setValue("zip", "12345-6789");
        assertTrue(form.validate(), "Form with valid extended ZIP code should pass");
    }

    @Test
    @DisplayName("Test URL validation")
    void testUrlValidation() {
        DefaultForm form = new DefaultForm();
        form.field("website").url("Invalid URL");

        // Invalid: incorrect URL
        form.setValue("website", "not-a-url");
        assertFalse(form.validate(), "Form with invalid URL should fail");
        assertEquals("Invalid URL", form.errors().get("website"), "Should have URL error");

        // Valid: correct URL
        form.setValue("website", "https://example.com");
        assertTrue(form.validate(), "Form with valid URL should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test date validation")
    void testDateValidation() {
        DefaultForm form = new DefaultForm();
        form.field("dob").date("Invalid date");

        // Invalid: incorrect date
        form.setValue("dob", "2023-13-01");
        assertFalse(form.validate(), "Form with invalid date should fail");
        assertEquals("Invalid date", form.errors().get("dob"), "Should have date error");

        // Valid: correct ISO date
        form.setValue("dob", "2023-05-01");
        assertTrue(form.validate(), "Form with valid ISO date should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test custom date pattern validation")
    void testCustomDateValidation() {
        DefaultForm form = new DefaultForm();
        form.field("event").date("MM/dd/yyyy", "Invalid date format");

        // Invalid: incorrect format
        form.setValue("event", "2023-05-01");
        assertFalse(form.validate(), "Form with invalid date format should fail");
        assertEquals("Invalid date format", form.errors().get("event"), "Should have date format error");

        // Valid: correct format
        form.setValue("event", "05/01/2023");
        assertTrue(form.validate(), "Form with valid custom date format should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test credit card validation")
    void testCreditCardValidation() {
        DefaultForm form = new DefaultForm();
        form.field("card").creditCard("Invalid credit card");

        // Invalid: incorrect card number
        form.setValue("card", "123456789012");
        assertFalse(form.validate(), "Form with invalid credit card should fail");
        assertEquals("Invalid credit card", form.errors().get("card"), "Should have credit card error");

        // Valid: correct card number (example Visa)
        form.setValue("card", "4532015112830366");
        assertTrue(form.validate(), "Form with valid credit card should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test strong password validation")
    void testStrongPasswordValidation() {
        DefaultForm form = new DefaultForm();
        form.field("password").strongPassword("Password too weak");

        // Invalid: weak password
        form.setValue("password", "weak");
        assertFalse(form.validate(), "Form with weak password should fail");
        assertEquals("Password too weak", form.errors().get("password"), "Should have strong password error");

        // Valid: strong password
        form.setValue("password", "Abcd1234!");
        assertTrue(form.validate(), "Form with strong password should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test IP address validation")
    void testIpAddressValidation() {
        DefaultForm form = new DefaultForm();
        form.field("ip").ipAddress("Invalid IP address");

        // Invalid: incorrect IP
        form.setValue("ip", "256.1.2.3");
        assertFalse(form.validate(), "Form with invalid IP address should fail");
        assertEquals("Invalid IP address", form.errors().get("ip"), "Should have IP address error");

        // Valid: correct IP
        form.setValue("ip", "192.168.1.1");
        assertTrue(form.validate(), "Form with valid IP address should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test numeric comparison validation")
    void testNumericValidation() {
        DefaultForm form = new DefaultForm();
        form.field("age").greaterThan(18, "Must be over 18")
                .lowerEqualsThan(100, "Must be 100 or less");

        // Invalid: too low
        form.setValue("age", "17");
        assertFalse(form.validate(), "Form with too low age should fail");
        assertEquals("Must be over 18", form.errors().get("age"), "Should have greaterThan error");

        // Invalid: too high
        form.setValue("age", "101");
        assertFalse(form.validate(), "Form with too high age should fail");
        assertEquals("Must be 100 or less", form.errors().get("age"), "Should have lowerEqualsThan error");

        // Valid: within range
        form.setValue("age", "25");
        assertTrue(form.validate(), "Form with valid age should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test clamp validation")
    void testClampValidation() {
        DefaultForm form = new DefaultForm();
        form.field("score").clamp(0, 100, "Score must be between 0 and 100");

        // Invalid: below range
        form.setValue("score", "-1");
        assertFalse(form.validate(), "Form with score below range should fail");
        assertEquals("Score must be between 0 and 100", form.errors().get("score"), "Should have clamp error");

        // Invalid: above range
        form.setValue("score", "101");
        assertFalse(form.validate(), "Form with score above range should fail");
        assertEquals("Score must be between 0 and 100", form.errors().get("score"), "Should have clamp error");

        // Valid: within range
        form.setValue("score", "75");
        assertTrue(form.validate(), "Form with valid score should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test regex validation")
    void testRegexValidation() {
        DefaultForm form = new DefaultForm();
        form.field("code").regex("^[A-Z]{3}$", "Must be three uppercase letters");

        // Invalid: incorrect pattern
        form.setValue("code", "abc");
        assertFalse(form.validate(), "Form with invalid code should fail");
        assertEquals("Must be three uppercase letters", form.errors().get("code"), "Should have regex error");

        // Valid: correct pattern
        form.setValue("code", "XYZ");
        assertTrue(form.validate(), "Form with valid code should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test custom rule validation")
    void testCustomRuleValidation() {
        DefaultForm form = new DefaultForm();
        form.field("input").rule(s -> s != null && s.startsWith("x"), "Must start with 'x'");

        // Invalid: does not start with 'x'
        form.setValue("input", "y123");
        assertFalse(form.validate(), "Form with invalid input should fail");
        assertEquals("Must start with 'x'", form.errors().get("input"), "Should have custom rule error");

        // Valid: starts with 'x'
        form.setValue("input", "x123");
        assertTrue(form.validate(), "Form with valid input should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test multiple errors on single field")
    void testMultipleErrors() {
        DefaultForm form = new DefaultForm();
        form.field("username").required("Username is required")
                .min(5, "Minimum 5 characters")
                .alphanumeric("Only letters and digits");

        form.setValue("username", "#");
        assertFalse(form.validate(), "Form with multiple invalid rules should fail");
        assertEquals("Username is required", form.errors().get("username"), "Should have first error");
        assertEquals(List.of("Username is required"), form.allErrors().get("username"),
                "Should have only required error");
    }

    @Test
    @DisplayName("Test multiple fields with errors")
    void testMultipleFields() {
        DefaultForm form = new DefaultForm();
        form.field("email").required("Email is required").email("Invalid email");
        form.field("age").greaterThan(18, "Must be over 18");

        form.setValue("email", "invalid").setValue("age", "17");
        assertFalse(form.validate(), "Form with multiple invalid fields should fail");
        assertEquals(Map.of("email", "Email is required", "age", "Must be over 18"),
                form.errors(), "Should have first errors for each field");
        assertEquals(Map.of("email", List.of("Email is required"),
                "age", List.of("Must be over 18")),
                form.allErrors(), "Should have all errors for each field");

        // Valid case
        form.setValue("email", "test@example.com").setValue("age", "25");
        assertTrue(form.validate(), "Form with valid fields should pass");
        assertTrue(form.errors().isEmpty(), "Valid form should have no errors");
    }

    @Test
    @DisplayName("Test type conversion methods")
    void testTypeConversion() {
        DefaultForm form = new DefaultForm();
        form.setValue("age", "42")
                .setValue("price", "19.99")
                .setValue("active", "true")
                .setValue("dob", "2023-05-01")
                .setValue("event", "05/01/2023");

        Field ageField = form.field("age");
        assertEquals(42, ageField.asInt(), "Should convert to correct integer");

        Field priceField = form.field("price");
        assertEquals(19.99, priceField.asDouble(), "Should convert to correct double");

        Field activeField = form.field("active");
        assertTrue(activeField.asBoolean(), "Should convert to correct boolean");

        Field dobField = form.field("dob");
        assertEquals(LocalDate.of(2023, 5, 1), dobField.asDate(), "Should convert to correct ISO date");

        Field eventField = form.field("event");
        assertEquals(LocalDate.of(2023, 5, 1), eventField.asDate("MM/dd/yyyy"),
                "Should convert to correct custom date");
    }

    @Test
    @DisplayName("Test type conversion with invalid inputs")
    void testInvalidTypeConversion() {
        DefaultForm form = new DefaultForm();
        form.setValue("age", "not-a-number")
                .setValue("price", "invalid")
                .setValue("dob", "invalid-date");

        Field ageField = form.field("age");
        assertThrows(NumberFormatException.class, ageField::asInt,
                "Should throw NumberFormatException for invalid integer");

        Field priceField = form.field("price");
        assertThrows(NumberFormatException.class, priceField::asDouble,
                "Should throw NumberFormatException for invalid double");

        Field dobField = form.field("dob");
        assertThrows(Exception.class, dobField::asDate, "Should throw exception for invalid ISO date");
    }

    @Test
    @DisplayName("Test error map immutability")
    void testErrorMapImmutability() {
        DefaultForm form = new DefaultForm();
        form.field("name").required("Name is required");
        form.validate();

        Map<String, String> errors = form.errors();
        Map<String, List<String>> allErrors = form.allErrors();

        assertThrows(UnsupportedOperationException.class, () -> errors.put("name", "test"),
                "Errors map should be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> allErrors.put("name", List.of("test")),
                "All errors map should be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> allErrors.get("name").add("test"),
                "All errors list should be unmodifiable");
    }
}