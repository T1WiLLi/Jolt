package io.github.t1willi.schedule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * An annotation for marking methods to be executed on a schedule.
 * <p>
 * The {@code Scheduled} annotation is used to configure methods for periodic or
 * scheduled execution
 * in a Java application, typically within a scheduling framework. It supports
 * both fixed-rate/fixed-delay
 * scheduling and cron-based scheduling. When applied to a method, it indicates
 * that the method should
 * be invoked automatically according to the specified schedule, defined by
 * attributes such as fixed
 * rate, time unit, initial delay, or cron expression. The annotation is
 * retained at runtime, allowing
 * the scheduling framework to inspect and process it dynamically. It is
 * typically used in enterprise
 * applications for tasks such as periodic data processing, monitoring, or
 * cleanup.
 *
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {

    /**
     * Specifies the fixed interval for scheduling the annotated method, in the unit
     * defined by {@link #timeUnit()}.
     * <p>
     * This attribute defines the time interval between successive executions of the
     * annotated method,
     * either as a fixed-rate (time between the start of executions) or fixed-delay
     * (time between the
     * end of one execution and the start of the next), depending on the scheduling
     * framework's
     * implementation. A value of -1 (default) indicates that fixed scheduling is
     * not used, and the
     * schedule is determined by the {@link #cron()} attribute instead. If both
     * {@code fixed} and
     * {@code cron} are specified, the framework may prioritize one based on its
     * configuration.
     *
     * @return the fixed interval in the specified time unit, or -1 if not used
     * @since 1.0.0
     */
    long fixed() default -1;

    /**
     * Specifies the time unit for the {@link #fixed()} and {@link #initialDelay()}
     * attributes.
     * <p>
     * This attribute defines the unit of time (e.g., milliseconds, seconds) used
     * for the {@code fixed}
     * and {@code initialDelay} values. It defaults to {@link TimeUnit#MILLISECONDS}
     * for fine-grained
     * control. The time unit applies to both the fixed interval and initial delay
     * to ensure consistent
     * scheduling configuration.
     *
     * @return the {@link TimeUnit} for the fixed interval and initial delay
     * @since 1.0.0
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * Specifies the initial delay before the first execution of the annotated
     * method.
     * <p>
     * This attribute defines the delay, in the unit specified by
     * {@link #timeUnit()}, before the first
     * execution of the scheduled method. A value of 0 (default) indicates no
     * initial delay, meaning the
     * method will be executed immediately when the scheduler starts. This is useful
     * for tasks that need
     * to start after a specific delay, such as waiting for system initialization.
     *
     * @return the initial delay in the specified time unit, or 0 for no delay
     * @since 1.0.0
     */
    long initialDelay() default 0;

    /**
     * Specifies a cron expression for scheduling the annotated method.
     * <p>
     * This attribute defines a cron expression to schedule the method's execution
     * at specific times or
     * intervals, following standard cron syntax (e.g., "0 0 * * * ?" for hourly
     * execution). An empty
     * string (default) indicates that cron-based scheduling is not used, and the
     * schedule is determined
     * by the {@link #fixed()} attribute instead. If both {@code cron} and
     * {@code fixed} are specified,
     * the framework may prioritize one based on its configuration.
     *
     * @return the cron expression for scheduling, or an empty string if not used
     * @since 1.0.0
     */
    String cron() default "";
}