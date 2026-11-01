package com.ase.wastems.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

/** Verifies the role hierarchy of D-026. */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private RoleHierarchy hierarchy;

    private Set<String> reachable(String role) {
        return hierarchy
                .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(role)))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void adminInheritsPlanner() {
        assertTrue(reachable("ROLE_ADMIN").contains("ROLE_PLANNER"));
    }

    @Test
    void plannerDoesNotInheritAdmin() {
        assertFalse(reachable("ROLE_PLANNER").contains("ROLE_ADMIN"));
    }

    @Test
    void agentInheritsNothing() {
        Set<String> r = reachable("ROLE_AGENT");
        assertFalse(r.contains("ROLE_ADMIN"));
        assertFalse(r.contains("ROLE_PLANNER"));
    }
}
