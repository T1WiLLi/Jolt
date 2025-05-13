package io.github.t1willi.mapper;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import io.github.t1willi.annotations.MapTo;

public final class MappingModule extends SimpleModule {

    public MappingModule(DtoMapper mapper) {
        setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(
                    SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {

                MapTo annotation = beanDesc.getBeanClass().getAnnotation(MapTo.class);
                if (annotation != null) {
                    return new JsonSerializer<Object>() {
                        @Override
                        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
                                throws IOException {
                            Object mapped = DtoMapperRegistry.map(value, annotation.value(), mapper);
                            serializers.defaultSerializeValue(mapped, gen);
                        }
                    };
                }
                return serializer;
            }
        });
    }
}
