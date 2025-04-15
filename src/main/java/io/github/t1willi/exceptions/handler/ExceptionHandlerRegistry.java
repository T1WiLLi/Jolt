package io.github.t1willi.exceptions.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.servlet.http.HttpServletResponse;

public final class ExceptionHandlerRegistry {
    private final Map<Class<? extends Throwable>, List<BiConsumer<Throwable, HttpServletResponse>>> handlers = new HashMap<>();

    public <T extends Throwable> void registerHandler(Class<T> exceptionType,
            BiConsumer<T, HttpServletResponse> handler) {
        handlers.computeIfAbsent(exceptionType, k -> new ArrayList<>())
                .add((t, res) -> handler.accept(castThrowable(exceptionType, t), res));
    }

    public boolean handleSpecificException(Throwable t, HttpServletResponse response) {
        Class<?> exceptionClass = t.getClass();
        List<BiConsumer<Throwable, HttpServletResponse>> specificHandlers = null;

        for (Class<?> registeredType : handlers.keySet()) {
            if (registeredType.isAssignableFrom(exceptionClass)) {
                specificHandlers = handlers.get(registeredType);
                break;
            }
        }

        if (specificHandlers != null) {
            for (BiConsumer<Throwable, HttpServletResponse> handler : specificHandlers) {
                handler.accept(t, response);
            }
            return true;
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
