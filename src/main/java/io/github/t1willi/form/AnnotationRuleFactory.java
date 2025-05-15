package io.github.t1willi.form;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import io.github.t1willi.annotations.Check;
import io.github.t1willi.annotations.Email;
import io.github.t1willi.annotations.Password;
import io.github.t1willi.annotations.Regex;
import io.github.t1willi.annotations.Required;
import io.github.t1willi.annotations.Size;
import io.github.t1willi.exceptions.FormConversionException;

public final class AnnotationRuleFactory {

    @SuppressWarnings("rawtypes")
    private static final FieldAnnotation[] FIELD_ANNOTATIONS = {
            new FieldAnnotation<>(Size.class, (f, sz) -> f.min(sz.min(), sz.message()).max(sz.max(), sz.message())),
            new FieldAnnotation<>(Required.class, (f, rq) -> f.required(rq.message())),
            new FieldAnnotation<>(Check.class, (f, ck) -> {
                try {
                    @SuppressWarnings("unchecked")
                    Predicate<String> p = (Predicate<String>) ck.value()
                            .getDeclaredConstructor().newInstance();
                    f.rule(p, ck.message());
                } catch (Exception e) {
                    throw new FormConversionException("Invalid check annotation: " + ck.value().getName(), e);
                }
            }),
            new FieldAnnotation<>(Regex.class, (f, pt) -> f.regex(pt.regex(), pt.message())),
            new FieldAnnotation<>(Email.class, (f, em) -> f.email(em.message())),
            new FieldAnnotation<>(Password.class, (f, pw) -> {
                f.min(pw.min(), pw.message()).max(pw.max(), pw.message());
                StringBuilder rx = new StringBuilder("^");
                if (pw.requireUppercase())
                    rx.append("(?=.*[A-Z])");
                if (pw.requireLowercase())
                    rx.append("(?=.*[a-z])");
                if (pw.requireDigit())
                    rx.append("(?=.*\\d)");
                if (pw.requireSpecial())
                    rx.append("(?=.*[^A-Za-z0-9])");
                rx.append(".*$");
                f.regex(rx.toString(), pw.message());
            })
    };

    public static <T> Form applyRules(Form form, Class<T> dtoClass) {
        for (java.lang.reflect.Field fld : dtoClass.getDeclaredFields()) {
            fld.setAccessible(true);
            Field field = form.field(fld.getName());
            for (Annotation annotation : fld.getAnnotations()) {
                find(annotation)
                        .ifPresent(fa -> fa.process(field, annotation));
            }
        }
        return form;
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> Optional<FieldAnnotation<A>> find(Annotation ann) {
        return Arrays.stream(FIELD_ANNOTATIONS)
                .filter(fa -> fa.annotationClass.equals(ann.annotationType()))
                .map(fa -> (FieldAnnotation<A>) fa)
                .findFirst();
    }

    private static class FieldAnnotation<T extends Annotation> {
        final Class<T> annotationClass;
        final java.util.function.BiConsumer<Field, T> consumer;

        FieldAnnotation(Class<T> ac, java.util.function.BiConsumer<Field, T> cons) {
            annotationClass = ac;
            consumer = cons;
        }

        void process(Field f, Annotation ann) {
            consumer.accept(f, annotationClass.cast(ann));
        }
    }
}
