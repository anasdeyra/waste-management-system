package com.ase.wastems.web.dto;

import com.ase.wastems.domain.AppUser;
import java.util.UUID;

/** The account linked to an agent (never the password hash). */
public record LinkedUserResponse(UUID id, String username, boolean active) {

    public static LinkedUserResponse from(AppUser u) {
        return new LinkedUserResponse(u.getId(), u.getUsername(), u.isActive());
    }
}
