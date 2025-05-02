package io.github.t1willi.http.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.utils.JacksonUtil;

import java.io.IOException;

public final class DefaultJsonSerializer implements JsonSerializer {
    private final ObjectMapper mapper = JacksonUtil.getObjectMapper();

    @Override
    public <T> T fromJson(byte[] data, Class<T> cls) throws IOException {
        return mapper.readValue(data, cls);
    }

    @Override
    public <T> T fromJson(byte[] data, TypeReference<T> type) throws IOException {
        return mapper.readValue(data, type);
    }

    @Override
    public byte[] toJson(Object obj) throws IOException {
        return mapper.writeValueAsBytes(obj);
    }

    @Override
    public ObjectMapper getMapper() {
        return mapper;
    }
}
