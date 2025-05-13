package io.github.t1willi.mapper;

import io.github.t1willi.annotations.MapField;
import io.github.t1willi.annotations.MapTo;
import io.github.t1willi.exceptions.DtoMappingException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class BeanDtoMapper implements DtoMapper {
    private static final ThreadLocal<Integer> recursionDepth = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_DEPTH = 10; // MAX_DEPT for recursive check upon a given @MapTo. otherwise check ur
                                             // impl ngl.

    @Override
    public <S, T> T map(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        recursionDepth.set(recursionDepth.get() + 1);
        if (recursionDepth.get() > MAX_DEPTH) {
            recursionDepth.remove();
            throw new DtoMappingException(
                    String.format("Maximum recursion depth exceeded mapping %s to %s: possible cyclic @MapTo",
                            source.getClass().getName(), targetClass.getName()));
        }
        try {
            return DtoMapperRegistry.map(source, targetClass, this);
        } finally {
            recursionDepth.set(recursionDepth.get() - 1);
            if (recursionDepth.get() == 0) {
                recursionDepth.remove();
            }
        }
    }

    public <S, T> T mapDirect(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        List<String> errors = new ArrayList<>();
        T dto;
        try {
            dto = targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new DtoMappingException(
                    String.format("Failed to instantiate target class %s: %s",
                            targetClass.getName(), e.getMessage()),
                    e);
        }

        Map<String, Field> srcMap = new HashMap<>();
        for (Class<?> c = source.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field sf : c.getDeclaredFields()) {
                if (Modifier.isStatic(sf.getModifiers())) {
                    continue;
                }
                srcMap.put(sf.getName().toLowerCase(), sf);
            }
        }

        for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field tf : c.getDeclaredFields()) {
                if (Modifier.isStatic(tf.getModifiers())) {
                    continue;
                }
                String tgtName = tf.getName();
                String key = tgtName.toLowerCase();

                String matchedKey = null;
                Field overrideSrc = null;
                for (Field possible : srcMap.values()) {
                    MapField mf = possible.getAnnotation(MapField.class);
                    if (mf != null && mf.value().equalsIgnoreCase(tgtName)) {
                        overrideSrc = possible;
                        matchedKey = possible.getName().toLowerCase();
                        break;
                    }
                }
                if (overrideSrc == null && srcMap.containsKey(key)) {
                    matchedKey = key;
                }

                if (matchedKey == null) {
                    continue;
                }

                Field sf = srcMap.get(matchedKey);
                sf.setAccessible(true);
                Object val;
                try {
                    val = sf.get(source);
                } catch (Exception e) {
                    errors.add(String.format("Failed to read source field %s.%s: %s",
                            source.getClass().getName(), sf.getName(), e.getMessage()));
                    continue;
                }

                if (val != null && val.getClass().isAnnotationPresent(MapTo.class)) {
                    MapTo mapTo = val.getClass().getAnnotation(MapTo.class);
                    if (mapTo == null || mapTo.value() == null) {
                        errors.add(String.format("Invalid @MapTo annotation on source field %s.%s",
                                source.getClass().getName(), sf.getName()));
                        continue;
                    }
                    Class<?> nestedT = mapTo.value();
                    if (nestedT == targetClass && val.getClass() == source.getClass()) {
                        errors.add(String.format("Recursive @MapTo detected on field %s.%s: mapping %s to itself",
                                source.getClass().getName(), sf.getName(), val.getClass().getName()));
                        continue;
                    }
                    try {
                        val = mapDirect(val, nestedT);
                    } catch (Exception e) {
                        errors.add(String.format("Failed to map nested field %s.%s from %s to %s: %s",
                                source.getClass().getName(), sf.getName(),
                                val.getClass().getName(), nestedT.getName(), e.getMessage()));
                        continue;
                    }
                } else if (val instanceof Collection) {
                    Collection<?> srcCol = (Collection<?>) val;
                    Collection<Object> dstCol = createCollection(srcCol, tf);
                    int i = 0;
                    for (Object item : srcCol) {
                        if (item != null && item.getClass().isAnnotationPresent(MapTo.class)) {
                            MapTo mapTo = item.getClass().getAnnotation(MapTo.class);
                            if (mapTo == null || mapTo.value() == null) {
                                errors.add(String.format(
                                        "Invalid @MapTo annotation on collection item %d in source field %s.%s",
                                        i, source.getClass().getName(), sf.getName()));
                                continue;
                            }
                            Class<?> nt = mapTo.value();
                            if (nt == targetClass && item.getClass() == source.getClass()) {
                                errors.add(String.format(
                                        "Recursive @MapTo detected on collection item %d in field %s.%s: mapping %s to itself",
                                        i, source.getClass().getName(), sf.getName(), item.getClass().getName()));
                                continue;
                            }
                            try {
                                dstCol.add(mapDirect(item, nt));
                            } catch (Exception e) {
                                errors.add(String.format(
                                        "Failed to map collection item %d in field %s.%s from %s to %s: %s",
                                        i, source.getClass().getName(), sf.getName(),
                                        item.getClass().getName(), nt.getName(), e.getMessage()));
                            }
                        } else {
                            dstCol.add(item);
                        }
                        i++;
                    }
                    val = dstCol;
                }

                tf.setAccessible(true);
                try {
                    if (!isAssignable(val, tf.getType())) {
                        errors.add(String.format("Type mismatch setting field %s.%s: cannot assign %s to %s",
                                targetClass.getName(), tf.getName(),
                                val != null ? val.getClass().getName() : "null", tf.getType().getName()));
                        continue;
                    }
                    tf.set(dto, val);
                } catch (IllegalArgumentException e) {
                    errors.add(String.format("Failed to set target field %s.%s with value of type %s: %s",
                            targetClass.getName(), tf.getName(),
                            val != null ? val.getClass().getName() : "null", e.getMessage()));
                } catch (Exception e) {
                    errors.add(String.format("Unexpected error setting field %s.%s with value of type %s: %s",
                            targetClass.getName(), tf.getName(),
                            val != null ? val.getClass().getName() : "null", e.getMessage()));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new DtoMappingException(
                    String.format("Failed to map %s to %s. Errors:\n  - %s",
                            source.getClass().getName(), targetClass.getName(), String.join("\n  - ", errors)));
        }
        return dto;
    }

    private boolean isAssignable(Object value, Class<?> targetType) {
        if (value == null) {
            return !targetType.isPrimitive();
        }
        Class<?> valueType = value.getClass();
        if (targetType.isAssignableFrom(valueType)) {
            return true;
        }
        if (targetType == boolean.class && valueType == Boolean.class) {
            return true;
        } else if (targetType == int.class && valueType == Integer.class) {
            return true;
        } else if (targetType == long.class && valueType == Long.class) {
            return true;
        } else if (targetType == double.class && valueType == Double.class) {
            return true;
        } else if (targetType == float.class && valueType == Float.class) {
            return true;
        } else if (targetType == short.class && valueType == Short.class) {
            return true;
        } else if (targetType == byte.class && valueType == Byte.class) {
            return true;
        } else if (targetType == char.class && valueType == Character.class) {
            return true;
        }
        return false;
    }

    private Collection<Object> createCollection(Collection<?> src, Field targetField) {
        Class<?> targetFieldType = targetField.getType();
        if (!Collection.class.isAssignableFrom(targetFieldType)) {
            throw new DtoMappingException(
                    String.format("Target field %s.%s is not a Collection: %s",
                            targetField.getDeclaringClass().getName(), targetField.getName(),
                            targetFieldType.getName()));
        }
        if (List.class.isAssignableFrom(targetFieldType) || src instanceof List) {
            return new ArrayList<>();
        } else if (Set.class.isAssignableFrom(targetFieldType) || src instanceof Set) {
            return new LinkedHashSet<>();
        } else if (Queue.class.isAssignableFrom(targetFieldType) || src instanceof Queue) {
            return new LinkedList<>();
        }
        return new ArrayList<>();
    }
}