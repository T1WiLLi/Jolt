package ca.jolt.tomcat.abstraction;

public abstract class ConfigurationBuilder<T> {

    public abstract T build();

    protected abstract void validate();
}
