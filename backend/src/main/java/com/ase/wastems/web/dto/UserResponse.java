package com.ase.wastems.web.dto;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import java.util.UUID;

/** An account as listed to administrators. Never carries the password hash. */
public record UserResponse(UUID id, String username, AppRole role, boolean active) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.isActive());
    }
}
