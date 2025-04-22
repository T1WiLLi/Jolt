package io.github.t1willi.security.role;

import io.github.t1willi.injector.JoltContainer;
import java.util.Set;

public final class Roles {
    private Roles() {
        /* no‑op */
    }

    private static class Holder {
        static final RoleManager MANAGER = JoltContainer.getInstance().getBean(RoleManager.class);
    }

    public static Role of(String name) {
        return Holder.MANAGER.from(name);
    }

    public static Role[] all() {
        return Holder.MANAGER.allRoles();
    }

    public static boolean implies(Role a, Role b) {
        return Holder.MANAGER.implies(a, b);
    }

    public static Set<Role> parents(Role r) {
        return Holder.MANAGER.getAllParents(r);
    }
}
