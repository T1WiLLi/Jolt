package ca.jolt.tomcat.abstraction;

public interface ConfigurationBuilder<T> {

    T build();

    void validate();
}
