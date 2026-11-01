package com.ase.wastems.security;

import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads accounts from the database for authentication. Usernames are case-insensitive. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String raw) {
        String username = raw.trim().toLowerCase(Locale.ROOT);
        AppUser u =
                users.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .disabled(!u.isActive())
                .build();
    }
}
