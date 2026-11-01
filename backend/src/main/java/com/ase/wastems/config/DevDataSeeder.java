package com.ase.wastems.config;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the development accounts (admin, planner, agent) in the {@code dev} profile.
 * Idempotent; the passwords are development credentials only and are never logged.
 */
@Component
@Profile("dev")
@Order(10)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DevDataSeeder(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", "admin123", AppRole.ADMIN);
        seed("planner", "planner123", AppRole.PLANNER);
        seed("agent", "agent123", AppRole.AGENT);
    }

    private void seed(String username, String password, AppRole role) {
        if (!users.existsByUsername(username)) {
            users.save(new AppUser(username, encoder.encode(password), role));
            LOG.info("Created development account '{}' with role {}", username, role);
        }
    }
}
