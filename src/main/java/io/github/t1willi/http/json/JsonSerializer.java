package io.github.t1willi.http.json;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.utils.JacksonUtil;

public interface JsonSerializer {
    <T> T fromJson(byte[] data, Class<T> cls) throws IOException;

    <T> T fromJson(byte[] data, TypeReference<T> type) throws IOException;

    byte[] toJson(Object obj) throws IOException;

    default ObjectMapper getMapper() {
        return JacksonUtil.getObjectMapper();
    }
}
