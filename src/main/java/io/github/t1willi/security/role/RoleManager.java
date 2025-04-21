package io.github.t1willi.security.role;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.JoltBean;
import jakarta.annotation.PostConstruct;

@JoltBean
public class RoleManager {
    private final Map<String, Role> lookup = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<RoleProvider> providers = JoltContainer.getInstance().getBeans(RoleProvider.class);
        for (RoleProvider p : providers) {
            for (Role r : p.provideRoles()) {
                register(r);
            }
        }
    }

    public void register(Role role) {
        Role existing = lookup.putIfAbsent(role.name(), role);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "Role already registered: " + role.name());
        }
    }

    public Role from(String name) {
        for (Map.Entry<String, Role> entry : lookup.entrySet()) {
            if (entry.getKey().toLowerCase().equals(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Role[] allRoles() {
        return lookup.values().toArray(new Role[0]);
    }

    public boolean implies(Role a, Role b) {
        return a.implies(b);
    }

    public Set<Role> getAllParents(Role role) {
        return Collections.unmodifiableSet(role.getImpliedRoles());
    }
}
