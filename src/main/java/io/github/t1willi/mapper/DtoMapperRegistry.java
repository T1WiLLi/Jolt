package io.github.t1willi.mapper;

import io.github.t1willi.exceptions.DtoMappingException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

public final class DtoMapperRegistry {
    private static final Logger LOGGER = Logger.getLogger(DtoMapperRegistry.class.getName());
    private static final Map<String, Function<Object, Object>> SUCCESS_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> FAILURE_CACHE = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    public static <S, T> T map(S source, Class<T> targetClass, DtoMapper fallback) {
        if (source == null) {
            return null;
        }
        String key = source.getClass().getName() + "->" + targetClass.getName();
        LOGGER.fine(
                () -> "Mapping " + source.getClass().getName() + " to " + targetClass.getName() + " with key " + key);

        if (FAILURE_CACHE.contains(key)) {
            LOGGER.warning(() -> "Mapping " + key + " previously failed; aborting");
            throw new DtoMappingException(
                    String.format("Mapping %s to %s previously failed; aborting to prevent recursive loop",
                            source.getClass().getName(), targetClass.getName()));
        }

        Function<S, T> fn = (Function<S, T>) SUCCESS_CACHE.get(key);
        if (fn != null) {
            LOGGER.fine(() -> "Using cached mapper for " + key);
            try {
                return fn.apply(source);
            } catch (Exception e) {
                LOGGER.severe(() -> "Failed to apply cached mapper for " + key + ": " + e.getMessage());
                throw new DtoMappingException(
                        String.format("Failed to apply cached mapper for %s to %s: %s",
                                source.getClass().getName(), targetClass.getName(), e.getMessage()),
                        e);
            }
        }

        LOGGER.fine(() -> "No cached mapper for " + key + "; calling fallback");
        try {
            // Use mapDirect directly to avoid recursive calls to map
            T dto = ((BeanDtoMapper) fallback).mapDirect(source, targetClass);
            if (dto != null) {
                SUCCESS_CACHE.put(key, s -> {
                    LOGGER.fine(() -> "Executing cached mapper for " + key);
                    return ((BeanDtoMapper) fallback).mapDirect((S) s, targetClass);
                });
            }
            return dto;
        } catch (DtoMappingException e) {
            FAILURE_CACHE.add(key);
            LOGGER.severe(() -> "Mapping failed for " + key + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            FAILURE_CACHE.add(key);
            LOGGER.severe(() -> "Unexpected error mapping " + key + ": " + e.getMessage());
            throw new DtoMappingException(
                    String.format("Failed to map %s to %s: %s",
                            source.getClass().getName(), targetClass.getName(), e.getMessage()),
                    e);
        }
    }

    private DtoMapperRegistry() {
        /* no instances */
    }
}