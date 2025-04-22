package io.github.t1willi.security.role;

import java.util.Set;

import io.github.t1willi.injector.annotation.JoltBean;

@JoltBean
public class DefaultRoles implements RoleProvider {
    public static final Role GUEST = Role.builder("GUEST").build();
    public static final Role USER = Role.builder("USER").parent(GUEST).build();
    public static final Role MODERATOR = Role.builder("MODERATOR").parent(USER).build();
    public static final Role ADMIN = Role.builder("ADMIN").parent(MODERATOR).build();
    public static final Role SUPER_ADMIN = Role.builder("SUPER_ADMIN").parent(ADMIN).build();
    public static final Role ROOT = Role.builder("ROOT").parent(SUPER_ADMIN).build();

    @Override
    public Set<Role> provideRoles() {
        return Set.of(GUEST, USER, MODERATOR, ADMIN, SUPER_ADMIN, ROOT);
    }
}
