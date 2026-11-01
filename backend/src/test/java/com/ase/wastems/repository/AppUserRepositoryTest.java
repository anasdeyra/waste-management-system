package com.ase.wastems.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Persistence tests of {@link AppUserRepository}. */
@DataJpaTest
@ActiveProfiles("test")
class AppUserRepositoryTest {

    @Autowired private AppUserRepository repository;

    @Test
    void savesAndReloadsUser() {
        repository.saveAndFlush(new AppUser("alice", "$2a$10$x", AppRole.PLANNER));
        AppUser found = repository.findByUsername("alice").orElseThrow();
        assertNotNull(found.getId());
        assertEquals(AppRole.PLANNER, found.getRole());
        assertTrue(found.isActive());
    }

    @Test
    void rejectsDuplicateUsername() {
        repository.saveAndFlush(new AppUser("bob", "h", AppRole.AGENT));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new AppUser("bob", "h2", AppRole.AGENT)));
    }
}
