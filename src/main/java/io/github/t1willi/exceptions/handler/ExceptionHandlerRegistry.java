package io.github.t1willi.exceptions.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.github.t1willi.exceptions.JoltException;
import jakarta.servlet.http.HttpServletResponse;

public final class ExceptionHandlerRegistry {
    private final Map<Class<? extends Throwable>, List<BiConsumer<Throwable, HttpServletResponse>>> handlers = new HashMap<>();

    public <T extends Throwable> void registerHandler(Class<T> exceptionType,
            BiConsumer<T, HttpServletResponse> handler) {
        handlers.computeIfAbsent(exceptionType, k -> new ArrayList<>())
                .add((t, res) -> handler.accept(castThrowable(exceptionType, t), res));
    }

    public void registerAnnotatedHandler(Object handlerInstance) {
        for (Method method : handlerInstance.getClass().getMethods()) {
            if (!method.isAnnotationPresent(HandleException.class)) {
                continue;
            }
            HandleException annotation = method.getAnnotation(HandleException.class);
            for (Class<? extends Throwable> exceptionType : annotation.value()) {
                registerHandler(exceptionType, (t, res) -> {
                    try {
                        if (method.getParameterCount() == 2
                                && method.getParameterTypes()[0].isAssignableFrom(t.getClass())
                                && method.getParameterTypes()[1].isAssignableFrom(HttpServletResponse.class)) {
                            method.invoke(handlerInstance, t, res);
                        } else {
                            throw new IllegalArgumentException(
                                    "Method " + method + " does not match expected signature.");
                        }
                    } catch (Exception e) {
                        throw new JoltException("Failed to invoke exception handler method " + method, e);
                    }
                });
            }
        }
    }

    public boolean handleSpecificException(Throwable t, HttpServletResponse response) {
        Class<?> exceptionClass = t.getClass();
        for (Map.Entry<Class<? extends Throwable>, List<BiConsumer<Throwable, HttpServletResponse>>> entry : handlers
                .entrySet()) {
            if (entry.getKey().isAssignableFrom(exceptionClass)) {
                for (BiConsumer<Throwable, HttpServletResponse> h : entry.getValue()) {
                    h.accept(t, response);
                }
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> T castThrowable(Class<T> type, Throwable throwable) {
        if (type.isInstance(throwable)) {
            return (T) throwable;
        } else {
            throw new IllegalArgumentException("Cannot cast " + throwable.getClass() + " to " + type);
        }
    }
}
