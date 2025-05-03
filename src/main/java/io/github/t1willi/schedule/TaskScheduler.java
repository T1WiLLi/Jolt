package io.github.t1willi.schedule;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.Autowire;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.injector.type.BeanScope;
import io.github.t1willi.injector.type.InitializationMode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Bean(initialization = InitializationMode.EAGER, scope = BeanScope.SINGLETON)
public final class TaskScheduler {
    private static final Logger logger = Logger.getLogger(TaskScheduler.class.getName());

    private ScheduledExecutorService scheduler;
    private CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    @Autowire
    private SchedulingConfiguration configuration;

    @PostConstruct
    public void start() {
        this.scheduler = Executors.newScheduledThreadPool(configuration.getPoolSize());
        List<Object> allBeans = JoltContainer.getInstance().getAllBeans();
        for (Object bean : allBeans) {
            for (Method method : bean.getClass().getMethods()) {
                if (method.isAnnotationPresent(Scheduled.class)) {
                    Scheduled annotation = method.getAnnotation(Scheduled.class);
                    Runnable task = createTask(bean, method);
                    if (!annotation.cron().isEmpty()) {
                        scheduleCronTask(task, annotation.cron());
                    } else {
                        this.scheduler.scheduleAtFixedRate(
                                task,
                                annotation.initialDelay(),
                                annotation.fixed(),
                                annotation.timeUnit());
                    }
                }
            }
        }
        logger.info("TaskScheduler initialized (poolSize=" + configuration.getPoolSize() + ")");
    }

    @PreDestroy
    public void shutdown() {
        this.scheduler.shutdownNow();
    }

    private Runnable createTask(Object bean, Method method) {
        return () -> {
            logger.info(() -> "Executing scheduled " +
                    bean.getClass().getSimpleName() + "#" + method.getName());

            try {
                method.invoke(bean);
                logger.info(() -> "Task executed successfully");
            } catch (Exception e) {
                logger.severe(() -> "Task execution failed: " + e.getMessage());
            }
        };
    }

    private void scheduleCronTask(Runnable task, String cronExpression) {
        Cron cron = parser.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        scheduleNext(task, executionTime);
    }

    private void scheduleNext(Runnable task, ExecutionTime executionTime) {
        Optional<Duration> next = executionTime.timeToNextExecution(ZonedDateTime.now());
        next.ifPresent(d -> {
            this.scheduler.schedule(() -> {
                task.run();
                scheduleNext(task, executionTime);
            },
                    d.toMillis(), TimeUnit.MILLISECONDS);
        });
    }
}
