package ca.jolt;

import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.injector.annotation.JoltBeanInjection;
import ca.jolt.injector.type.BeanScope;
import ca.jolt.injector.type.InitializationMode;

@JoltBean(scope = BeanScope.SINGLETON, initialization = InitializationMode.LAZY)
public class A {

    @JoltBeanInjection
    private B b;

    public void doWork() {
        System.out.println("A.doWork()");
        b.doWork();
    }
}
