package io.github.t1willi.injector.type;

/**
 * Enum representing the scope of a bean.
 */
public enum BeanScope {
    /**
     * Indicates that a single instance of the bean is created and shared across the
     * application.
     */
    SINGLETON,

    /**
     * Indicates that a new instance of the bean is created each time it is
     * requested.
     */
    PROTOTYPE
}