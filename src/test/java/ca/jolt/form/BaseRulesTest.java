package ca.jolt.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseRulesTest {

    @Test
    void testRequiredRule() {
        Rule rule = BaseRules.required("Field is required");
        assertTrue(rule.validate("Valid Data"));
        assertFalse(rule.validate(""));
        assertFalse(rule.validate(null));
    }

    @Test
    void testMinLengthRule() {
        Rule rule = BaseRules.minLength(5, "Minimum length is 5");
        assertTrue(rule.validate("Hello"));
        assertFalse(rule.validate("Hi"));
        assertFalse(rule.validate(null));
    }

    @Test
    void testMaxLengthRule() {
        Rule rule = BaseRules.maxLength(10, "Maximum length is 10");
        assertTrue(rule.validate("Short"));
        assertFalse(rule.validate("This is too long"));
        assertFalse(rule.validate(null));
    }

    @Test
    void testEmailRule() {
        Rule rule = BaseRules.email("Invalid email");
        assertTrue(rule.validate("test@example.com"));
        assertFalse(rule.validate("invalid-email"));
        assertFalse(rule.validate("example@.com"));
        assertFalse(rule.validate(null));
    }

    @Test
    void testAlphanumericRule() {
        Rule rule = BaseRules.alphanumeric("Only letters and numbers allowed");
        assertTrue(rule.validate("abc123"));
        assertFalse(rule.validate("abc 123"));
        assertFalse(rule.validate("special@!"));
    }

    @Test
    void testPhoneNumberRule() {
        Rule rule = BaseRules.phoneNumber("Invalid phone number");
        assertTrue(rule.validate("+1234567890"));
        assertTrue(rule.validate("(123) 456-7890"));
        assertFalse(rule.validate("invalid phone"));
        assertFalse(rule.validate("1234"));
    }

    @Test
    void testZipCodeRule() {
        Rule rule = BaseRules.zipCode("Invalid ZIP code");
        assertTrue(rule.validate("12345"));
        assertTrue(rule.validate("12345-6789"));
        assertFalse(rule.validate("1234"));
        assertFalse(rule.validate("12345-678"));
    }

    @Test
    void testUrlRule() {
        Rule rule = BaseRules.url("Invalid URL");
        assertTrue(rule.validate("http://example.com"));
        assertTrue(rule.validate("https://secure.com"));
        assertFalse(rule.validate("invalid-url"));
    }

    @Test
    void testDateRule_ISOFormat() {
        Rule rule = BaseRules.date("Invalid date format");
        assertTrue(rule.validate("2023-06-15"));
        assertFalse(rule.validate("15-06-2023"));
        assertFalse(rule.validate("invalid"));
    }

    @Test
    void testDateRule_CustomFormat() {
        Rule rule = BaseRules.date("MM/dd/yyyy", "Invalid custom date format");
        assertTrue(rule.validate("06/15/2023"));
        assertFalse(rule.validate("2023-06-15"));
        assertFalse(rule.validate("invalid date"));
    }

    @Test
    void testCreditCardRule() {
        Rule rule = BaseRules.creditCard("Invalid credit card");
        assertTrue(rule.validate("4111111111111111")); // Valid Luhn
        assertFalse(rule.validate("1234567890123456")); // Invalid Luhn
        assertFalse(rule.validate("invalid"));
    }

    @Test
    void testStrongPasswordRule() {
        Rule rule = BaseRules.strongPassword("Weak password");
        assertTrue(rule.validate("Aa1@strong"));
        assertFalse(rule.validate("weakpass"));
        assertFalse(rule.validate("12345678"));
        assertFalse(rule.validate("NoSpecial1"));
    }

    @Test
    void testIPAddressRule() {
        Rule rule = BaseRules.ipAddress("Invalid IP address");
        assertTrue(rule.validate("192.168.1.1"));
        assertFalse(rule.validate("999.999.999.999"));
        assertFalse(rule.validate("invalidIP"));
    }

    @Test
    void testLowerThanRule() {
        Rule rule = BaseRules.lowerThan(10, "Must be lower than 10");
        assertTrue(rule.validate("9"));
        assertFalse(rule.validate("10"));
        assertFalse(rule.validate("invalid"));
    }

    @Test
    void testLowerEqualsThanRule() {
        Rule rule = BaseRules.lowerEqualsThan(10, "Must be lower or equal to 10");
        assertTrue(rule.validate("10"));
        assertTrue(rule.validate("9"));
        assertFalse(rule.validate("11"));
    }

    @Test
    void testGreaterThanRule() {
        Rule rule = BaseRules.greaterThan(10, "Must be greater than 10");
        assertTrue(rule.validate("11"));
        assertFalse(rule.validate("10"));
        assertFalse(rule.validate("invalid"));
    }

    @Test
    void testGreaterEqualsThanRule() {
        Rule rule = BaseRules.greaterEqualsThan(10, "Must be greater or equal to 10");
        assertTrue(rule.validate("10"));
        assertTrue(rule.validate("11"));
        assertFalse(rule.validate("9"));
    }

    @Test
    void testClampRule() {
        Rule rule = BaseRules.clamp(5, 15, "Must be between 5 and 15");
        assertTrue(rule.validate("10"));
        assertFalse(rule.validate("4"));
        assertFalse(rule.validate("16"));
    }

    @Test
    void testRegexRule() {
        Rule rule = BaseRules.regex("^A.*Z$", "Must start with A and end with Z", "");
        assertTrue(rule.validate("AmazingZ"));
        assertFalse(rule.validate("BoringZ"));
        assertFalse(rule.validate("Amaze"));
    }
}
