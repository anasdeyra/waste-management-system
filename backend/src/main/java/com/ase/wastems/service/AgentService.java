package com.ase.wastems.service;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules of operational agents. Display names are not unique; the UUID is the identity. */
@Service
public class AgentService {

    private final AgentRepository agents;
    private final AppUserRepository users;

    public AgentService(AgentRepository agents, AppUserRepository users) {
        this.agents = agents;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Agent> list(ResourceStatus status) {
        return status == null
                ? agents.findAllByOrderByDisplayNameAscIdAsc()
                : agents.findByStatusOrderByDisplayNameAscIdAsc(status);
    }

    @Transactional(readOnly = true)
    public Agent get(UUID id) {
        return agents.findById(id).orElseThrow(() -> new NotFoundException("Agent not found: " + id));
    }

    @Transactional
    public Agent create(String displayName) {
        return agents.save(new Agent(displayName.trim(), DataOrigin.USER_ENTERED));
    }

    @Transactional
    public Agent update(UUID id, String displayName) {
        Agent a = get(id);
        a.setDisplayName(displayName.trim());
        return a;
    }

    @Transactional
    public Agent setStatus(UUID id, ResourceStatus status) {
        Agent a = get(id);
        a.setStatus(status);
        return a;
    }

    /** Agents that may be assigned to a new tour (US-17); S4 tour drafts use it. */
    @Transactional(readOnly = true)
    public List<Agent> findUsable() {
        return list(ResourceStatus.AVAILABLE);
    }
}
