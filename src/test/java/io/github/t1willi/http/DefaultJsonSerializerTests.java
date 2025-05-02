package io.github.t1willi.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.http.json.DefaultJsonSerializer;
import io.github.t1willi.utils.JacksonUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.List;

class DefaultJsonSerializerTests {

    private DefaultJsonSerializer serializer;
    private ObjectMapper mockMapper;

    private MockedStatic<JacksonUtil> jacksonUtilStatic;

    @BeforeEach
    void setUp() {
        mockMapper = mock(ObjectMapper.class);
        jacksonUtilStatic = mockStatic(JacksonUtil.class);
        jacksonUtilStatic
                .when(JacksonUtil::getObjectMapper)
                .thenReturn(mockMapper);

        serializer = new DefaultJsonSerializer();
    }

    @AfterEach
    void tearDown() {
        jacksonUtilStatic.close();
    }

    @Test
    @DisplayName("Test toJson serializes object")
    void testToJson() throws IOException {
        Object obj = new TestObject("test", 42);
        byte[] expectedBytes = "{\"name\":\"test\",\"value\":42}".getBytes();
        when(mockMapper.writeValueAsBytes(obj)).thenReturn(expectedBytes);

        byte[] result = serializer.toJson(obj);
        assertArrayEquals(expectedBytes, result, "toJson should serialize object to expected bytes");
    }

    @Test
    @DisplayName("Test fromJson with Class deserializes correctly")
    void testFromJsonClass() throws IOException {
        byte[] data = "{\"name\":\"test\",\"value\":42}".getBytes();
        TestObject expected = new TestObject("test", 42);
        when(mockMapper.readValue(data, TestObject.class)).thenReturn(expected);

        TestObject result = serializer.fromJson(data, TestObject.class);
        assertEquals(expected, result, "fromJson should deserialize to expected object");
    }

    @Test
    @DisplayName("Test fromJson with TypeReference deserializes correctly")
    void testFromJsonTypeReference() throws IOException {
        byte[] data = "[{\"name\":\"test\",\"value\":42}]".getBytes();
        List<TestObject> expected = List.of(new TestObject("test", 42));

        TypeReference<List<TestObject>> typeReference = new TypeReference<>() {
        };

        when(mockMapper.readValue(eq(data), eq(typeReference)))
                .thenReturn(expected);

        List<TestObject> result = serializer.fromJson(data, typeReference);

        assertEquals(expected, result, "fromJson should deserialize to expected list");
    }

    @Test
    @DisplayName("Test getMapper returns correct ObjectMapper")
    void testGetMapper() {
        assertEquals(mockMapper, serializer.getMapper(), "getMapper should return the mocked ObjectMapper");
    }

    private static class TestObject {
        private String name;
        private int value;

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestObject that = (TestObject) o;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + value;
        }
    }
}