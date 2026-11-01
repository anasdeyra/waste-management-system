package com.ase.wastems.service;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administration of accounts: creation and (de)activation. Accounts are never deleted. */
@Service
public class UserAdminService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AgentService agents;

    public UserAdminService(
            AppUserRepository users, PasswordEncoder encoder, AgentService agents) {
        this.users = users;
        this.encoder = encoder;
        this.agents = agents;
    }

    @Transactional(readOnly = true)
    public List<AppUser> list() {
        return users.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public AppUser get(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public AppUser create(String username, String rawPassword, AppRole role, UUID agentId) {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(normalized)) {
            throw new ConflictException("Username already exists: " + normalized);
        }
        if (agentId != null && role != AppRole.AGENT) {
            throw new BusinessRuleException(
                    "Only an AGENT account can be linked to an operational agent.");
        }
        AppUser saved = users.save(new AppUser(normalized, encoder.encode(rawPassword), role));
        if (agentId != null) {
            agents.linkUserAccount(agentId, saved.getId());
        }
        return saved;
    }

    @Transactional
    public AppUser setActive(UUID id, boolean active, String actingUsername) {
        AppUser user = get(id);
        if (!active && user.getUsername().equals(actingUsername)) {
            throw new ConflictException("You cannot deactivate your own account.");
        }
        user.setActive(active);
        return user;
    }
}
