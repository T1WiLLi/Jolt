package io.github.t1willi.injector.type;

/**
 * Enum representing the initialization mode of a bean.
 */
public enum InitializationMode {
    /**
     * Indicates that the bean is initialized immediately when the application
     * starts.
     */
    EAGER,

    /**
     * Indicates that the bean is initialized only when it is first requested.
     */
    LAZY
}