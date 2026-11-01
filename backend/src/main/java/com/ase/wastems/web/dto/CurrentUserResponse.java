package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import java.util.UUID;

/** The signed-in user as seen by the client. Never carries the password hash. */
public record CurrentUserResponse(UUID id, String username, AppRole role, UUID agentId) {

    /** {@code linked} is the operational agent linked to the account, or null. */
    public static CurrentUserResponse from(AppUser user, Agent linked) {
        return new CurrentUserResponse(
                user.getId(), user.getUsername(), user.getRole(), linked == null ? null : linked.getId());
    }
}
