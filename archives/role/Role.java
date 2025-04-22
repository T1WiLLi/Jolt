package io.github.t1willi.security.role;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class Role {
    private final String name;
    private final Set<Role> parents;

    private Role(String name, Set<Role> parents) {
        this.name = Objects.requireNonNull(name, "Role name cannot be null");
        this.parents = Collections.unmodifiableSet(new LinkedHashSet<>(parents));
    }

    public String name() {
        return this.name;
    }

    public Set<Role> parents() {
        return this.parents;
    }

    public Set<Role> getImpliedRoles() {
        Set<Role> result = new LinkedHashSet<>();
        Deque<Role> queue = new ArrayDeque<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            Role current = queue.remove();
            if (result.add(current)) {
                for (Role parent : current.parents()) {
                    if (!result.contains(parent)) {
                        queue.add(parent);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public boolean implies(Role other) {
        return getImpliedRoles().contains(other);
    }

    public boolean hasParent(Role role) {
        return this.parents.contains(role) || this.parents.stream().anyMatch(r -> r.hasParent(role));
    }

    @Override
    public String toString() {
        String inherited = getImpliedRoles().stream()
                .filter(role -> !role.equals(this))
                .map(Role::name)
                .collect(Collectors.joining(", ", "[", "]"));
        return name + " " + inherited;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final Set<Role> parents = new LinkedHashSet<>();

        public Builder(String name) {
            this.name = Objects.requireNonNull(name, "Role name cannot be null");
        }

        public Builder parent(Role parent) {
            this.parents.add(parent);
            return this;
        }

        public Builder parents(Role... parents) {
            this.parents.addAll(Set.of(parents));
            return this;
        }

        public Role build() {
            return new Role(name, parents);
        }
    }
}