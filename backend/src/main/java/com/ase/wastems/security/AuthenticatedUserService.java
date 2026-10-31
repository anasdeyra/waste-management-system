package com.ase.wastems.security;

import com.ase.wastems.repository.AppUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads the {@link AuthenticatedUser} that matches a username. */
@Service
public class AuthenticatedUserService {

    private final AppUserRepository userRepository;

    public AuthenticatedUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> findByUsername(String username) {
        return userRepository.findByUsername(username).map(AuthenticatedUser::new);
    }
}
