package com.ase.wastems.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** The dev seed is idempotent. */
@DataJpaTest
@ActiveProfiles("test")
class DevDataSeederTest {

    @Autowired private AppUserRepository repo;

    @Test
    void seedingTwiceCreatesThreeAccounts() {
        DevDataSeeder seeder = new DevDataSeeder(repo, new BCryptPasswordEncoder());
        seeder.run(null);
        seeder.run(null);
        assertEquals(3, repo.count());
        AppUser admin = repo.findByUsername("admin").orElseThrow();
        assertEquals(AppRole.ADMIN, admin.getRole());
        assertTrue(admin.isActive());
    }
}
