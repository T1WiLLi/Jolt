package io.github.t1willi.mapper;

public interface DtoMapper {
    <S, T> T map(S source, Class<T> target);
}
