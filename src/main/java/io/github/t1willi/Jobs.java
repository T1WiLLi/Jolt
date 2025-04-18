package io.github.t1willi;

import java.util.concurrent.TimeUnit;

import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.schedule.Scheduled;

@JoltBean
public class Jobs {

    @Scheduled(fixed = 5, timeUnit = TimeUnit.SECONDS)
    public void doSomething() {
        System.out.println("Doing something");
    }
}
