package io.github.t1willi.security.role;

import java.util.Set;

/**
 * Provides custom roles to the RoleRegistry. Must be registered as a JoltBean.
 */
public interface RoleProvider {

    /**
     * Supply a set of roles for registration.
     * 
     * @return a set of roles to be registered
     */
    Set<Role> provideRoles();

}
