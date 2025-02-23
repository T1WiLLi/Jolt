package ca.jolt.server.abstraction;

public interface ConfigurationBuilder<T> {

    T build();

    void validate();
}
