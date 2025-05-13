package io.github.t1willi.mapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.t1willi.exceptions.DtoMappingException;

public final class DtoMapperRegistry {
    private static final Map<String, Function<Object, Object>> SUCCESS_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> FAILURE_CACHE = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    public static <S, T> T map(S source, Class<T> targetClass, DtoMapper fallback) {
        if (source == null)
            return null;
        String key = source.getClass().getName() + "->" + targetClass.getName();

        if (FAILURE_CACHE.contains(key)) {
            throw new DtoMappingException(
                    "Previous attempt to map "
                            + source.getClass().getSimpleName()
                            + " → "
                            + targetClass.getSimpleName()
                            + " failed; aborting retry.");
        }

        Function<S, T> fn = (Function<S, T>) SUCCESS_CACHE.get(key);
        if (fn != null) {
            try {
                return fn.apply(source);
            } catch (Exception e) {
                throw new DtoMappingException(
                        "Error applying cached mapper for "
                                + key + ": " + e.getMessage(),
                        e);
            }
        }

        try {
            T dto = fallback.map(source, targetClass);
            SUCCESS_CACHE.put(key, s -> fallback.map((S) s, targetClass));
            return dto;
        } catch (Exception e) {
            FAILURE_CACHE.add(key);
            throw new DtoMappingException(
                    "Failed to map "
                            + source.getClass().getSimpleName()
                            + " → "
                            + targetClass.getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e);
        }
    }

    private DtoMapperRegistry() {
        /* no instances */
    }
}
