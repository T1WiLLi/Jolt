package ca.jolt;

import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.injector.type.BeanScope;

@JoltBean(scope = BeanScope.SINGLETON)
public class B {

    public void doWork() {
        System.out.println("B.doWork()");
    }
}
