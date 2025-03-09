package ca.jolt.form;

import ca.jolt.exceptions.FormConversionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Form}.
 */
class FormTest {

    private Form form;

    @BeforeEach
    void setUp() {
        form = new Form();
    }

    @Test
    void testSetValueAndGetFieldValue() {
        form.setValue("username", "testUser");
        assertEquals("testUser", form.getValue("username"));
    }

    @Test
    void testFieldValidatorCreation() {
        FieldValidator validator = form.field("email");
        assertNotNull(validator);
        assertSame(validator, form.field("email"));
    }

    @Test
    void testRequiredFieldValidation() {
        form.setValue("username", "");
        form.field("username").required();

        assertFalse(form.verify());
        assertEquals("This field is required.", form.getError("username"));
    }

    @Test
    void testMinLengthValidation() {
        form.setValue("password", "abc");
        form.field("password").minLength(6);

        assertFalse(form.verify());
        assertEquals("Value must be at least 6 characters long.", form.getError("password"));

        form.setValue("password", "abcdef");
        assertTrue(form.verify());
    }

    @Test
    void testMaxLengthValidation() {
        form.setValue("username", "verylongusername");
        form.field("username").maxLength(10);

        assertFalse(form.verify());
        assertEquals("Value must be at most 10 characters long.", form.getError("username"));

        form.setValue("username", "short");
        assertTrue(form.verify());
    }

    @Test
    void testEmailValidation() {
        form.setValue("email", "valid@example.com");
        form.field("email").email();
        assertTrue(form.verify());

        form.setValue("email", "invalid-email");
        assertFalse(form.verify());
        assertEquals("Invalid email format.", form.getError("email"));
    }

    @Test
    void testZipCodeValidation() {
        form.setValue("zip", "12345-6789");
        form.field("zip").zipCode();
        assertTrue(form.verify());

        form.setValue("zip", "1234");
        assertFalse(form.verify());
        assertEquals("Invalid zip code format.", form.getError("zip"));
    }

    @Test
    void testEntityConversion() {
        form.setValue("name", "John Doe");
        form.setValue("age", "30");

        TestEntity entity = form.buildEntity(TestEntity.class);

        assertEquals("John Doe", entity.getName());
        assertEquals(30, entity.getAge());
    }

    @Test
    void testEntityUpdate() {
        form.setValue("name", "Jane Doe");
        form.setValue("age", "25");

        TestEntity entity = new TestEntity();
        entity.setName("Old Name");
        entity.setAge(40);

        form.updateEntity(entity);

        assertEquals("Jane Doe", entity.getName());
        assertEquals(25, entity.getAge());
    }

    @Test
    void testIntegerConversion() {
        form.setValue("age", "25");
        assertEquals(25, form.getValueAsInt("age"));

        form.setValue("age", "invalid");
        assertThrows(FormConversionException.class, () -> form.getValueAsInt("age"));
    }

    @Test
    void testDoubleConversion() {
        form.setValue("price", "19.99");
        assertEquals(19.99, form.getValueAsDouble("price"));

        form.setValue("price", "invalid");
        assertThrows(FormConversionException.class, () -> form.getValueAsDouble("price"));
    }

    @Test
    void testBooleanConversion() {
        form.setValue("active", "true");
        assertTrue(form.getValueAsBoolean("active"));

        form.setValue("active", "false");
        assertFalse(form.getValueAsBoolean("active"));

        form.setValue("active", "");
        assertThrows(FormConversionException.class, () -> form.getValueAsBoolean("active"));
    }

    @Test
    void testDateConversion() {
        form.setValue("dob", "1990-01-01");
        assertEquals(LocalDate.of(1990, 1, 1), form.getValueAsDate("dob"));

        form.setValue("dob", "invalid-date");
        assertThrows(FormConversionException.class, () -> form.getValueAsDate("dob"));
    }

    @Test
    void testCustomDateFormatConversion() {
        form.setValue("dob", "01/01/1990");
        form.registerDatePattern("dob", "MM/dd/yyyy");

        assertEquals(LocalDate.of(1990, 1, 1), form.getValueAsDate("dob", "MM/dd/yyyy"));
    }

    @Test
    void testErrorFormatting() {
        form.setValue("username", "");
        form.field("username").required();
        form.verify();

        form.setErrorTemplate("Error in {field}: {message}");
        assertEquals("Error in username: This field is required.", form.getAllErrors().get(0));
    }

    @Test
    void testValidateFieldMethod() {
        FieldValidator validator = Form.validateField("username", "john_doe");
        assertNotNull(validator);
        assertEquals("username", validator.getFieldName());
    }

    /**
     * Test entity class for form conversion.
     */
    static class TestEntity {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
