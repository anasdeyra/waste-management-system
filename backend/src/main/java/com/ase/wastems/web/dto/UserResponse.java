package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import java.util.UUID;

/** An account as listed to administrators. Never carries the password hash. */
public record UserResponse(
        UUID id,
        String username,
        AppRole role,
        boolean active,
        UUID linkedAgentId,
        String linkedAgentName) {

    public static UserResponse from(AppUser u) {
        return from(u, null);
    }

    /** {@code linked} is the operational agent linked to the account, or null. */
    public static UserResponse from(AppUser u, Agent linked) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getRole(),
                u.isActive(),
                linked == null ? null : linked.getId(),
                linked == null ? null : linked.getDisplayName());
    }
}
