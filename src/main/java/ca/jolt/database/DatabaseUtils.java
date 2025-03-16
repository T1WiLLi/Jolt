package ca.jolt.database;

import java.util.Date;

import ca.jolt.database.annotation.Column;

class DatabaseUtils {

    public static Object convertToFieldType(String value, Class<?> targetType) {
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == String.class) {
            return value;
        } else if (targetType == java.util.UUID.class) {
            return java.util.UUID.fromString(value);
        } else {
            throw new IllegalArgumentException("Unsupported ID type: " + targetType.getName());
        }
    }

    public static String getSqlTypeForJavaType(Class<?> type, Column column) {
        if (type == String.class) {
            int length = column != null ? column.length() : 255;
            return "VARCHAR(" + length + ")";
        } else if (type == Integer.class || type == int.class) {
            return "INTEGER";
        } else if (type == Long.class || type == long.class) {
            return "BIGINT";
        } else if (type == Double.class || type == double.class) {
            return "DOUBLE PRECISION";
        } else if (type == Float.class || type == float.class) {
            return "REAL";
        } else if (type == Boolean.class || type == boolean.class) {
            return "BOOLEAN";
        } else if (type == Date.class) {
            return "TIMESTAMP";
        } else if (type == java.util.UUID.class) {
            return "UUID";
        } else {
            return "TEXT";
        }
    }
}
