package io.github.t1willi.mapper;

import io.github.t1willi.annotations.MapField;
import io.github.t1willi.annotations.MapTo;
import io.github.t1willi.exceptions.DtoMappingException;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class BeanDtoMapper implements DtoMapper {

    @Override
    public <S, T> T map(S source, Class<T> targetClass) {
        try {
            return mapDirect(source, targetClass);
        } catch (DtoMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new DtoMappingException(
                    "Mapping failed for "
                            + source.getClass().getSimpleName()
                            + " → "
                            + targetClass.getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e);
        }
    }

    private <S, T> T mapDirect(S source, Class<T> targetClass) throws Exception {
        T dto = targetClass.getDeclaredConstructor().newInstance();

        BeanInfo srcInfo = Introspector.getBeanInfo(source.getClass(), Object.class);
        Map<String, PropertyDescriptor> srcProps = Arrays.stream(srcInfo.getPropertyDescriptors())
                .filter(pd -> pd.getReadMethod() != null)
                .collect(Collectors.toMap(PropertyDescriptor::getName, pd -> pd));

        Map<String, Field> targetFields = Arrays.stream(targetClass.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .collect(Collectors.toMap(Field::getName, f -> f));

        Map<String, String> overrides = new HashMap<>();
        for (Field f : source.getClass().getDeclaredFields()) {
            MapField mf = f.getAnnotation(MapField.class);
            if (mf != null)
                overrides.put(mf.value(), f.getName());
        }

        List<String> missingGetters = new ArrayList<>();
        for (String targetName : targetFields.keySet()) {
            String srcName = overrides.getOrDefault(targetName, targetName);
            if (!srcProps.containsKey(srcName)) {
                missingGetters.add(srcName);
            }
        }

        if (!missingGetters.isEmpty()) {
            throw new DtoMappingException(
                    "No public getter found for source property(ies) in "
                            + source.getClass().getSimpleName() + ": " + missingGetters);
        }

        for (String targetName : targetFields.keySet()) {
            String srcName = overrides.getOrDefault(targetName, targetName);
            PropertyDescriptor srcPd = srcProps.get(srcName);
            if (srcPd == null) {
                continue;
            }

            Method getter = srcPd.getReadMethod();
            Object value = getter.invoke(source);

            if (value == null) {
                continue;
            } else if (value.getClass().isAnnotationPresent(MapTo.class)) {
                Class<?> nestedT = value.getClass().getAnnotation(MapTo.class).value();
                value = DtoMapperRegistry.map(value, nestedT, this);
            } else if (value instanceof Collection) {
                Collection<?> srcCol = (Collection<?>) value;
                Collection<Object> dstCol = createCollection(srcCol);
                for (Object item : srcCol) {
                    if (item != null && item.getClass().isAnnotationPresent(MapTo.class)) {
                        Class<?> nt = item.getClass().getAnnotation(MapTo.class).value();
                        dstCol.add(DtoMapperRegistry.map(item, nt, this));
                    } else {
                        dstCol.add(item);
                    }
                }
                value = dstCol;
            }

            Field field = targetFields.get(targetName);
            try {
                field.set(dto, value);
            } catch (IllegalAccessException e) {
                throw new DtoMappingException(
                        "Failed to set field " + targetName + " on " + targetClass.getSimpleName() + ": "
                                + e.getMessage(),
                        e);
            }
        }

        return dto;
    }

    private Collection<Object> createCollection(Collection<?> src) {
        if (src instanceof List)
            return new ArrayList<>();
        else if (src instanceof Set)
            return new LinkedHashSet<>();
        else if (src instanceof Queue)
            return new LinkedList<>();
        else
            return new ArrayList<>();
    }
}