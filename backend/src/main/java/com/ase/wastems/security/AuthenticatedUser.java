package com.ase.wastems.security;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated principal built from an {@link AppUser}.
 *
 * <p>It deliberately exposes only the information the application needs: the identifier, the
 * full name and the granted roles (NFR-11).
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String fullName;
    private final List<String> roles;
    private final String passwordHash;

    public AuthenticatedUser(AppUser user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.passwordHash = user.getPasswordHash();
        this.roles = user.getRoles().stream().map(AppRole::name).sorted().toList();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
