package com.ase.wastems.service;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules of operational agents. Display names are not unique; the UUID is the identity. */
@Service
public class AgentService {

    private final AgentRepository agents;
    private final AppUserRepository users;

    private final AuditService audit;

    public AgentService(AgentRepository agents, AppUserRepository users, AuditService audit) {
        this.audit = audit;
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
        Agent saved = agents.save(new Agent(displayName.trim(), DataOrigin.USER_ENTERED));
        audit.record(
                AuditAction.AGENT_CREATED, AuditEntityType.AGENT, saved.getId(), saved.getDisplayName());
        return saved;
    }

    @Transactional
    public Agent update(UUID id, String displayName) {
        Agent a = get(id);
        a.setDisplayName(displayName.trim());
        audit.record(
                AuditAction.AGENT_UPDATED, AuditEntityType.AGENT, a.getId(), a.getDisplayName());
        return a;
    }

    @Transactional
    public Agent setStatus(UUID id, ResourceStatus status) {
        Agent a = get(id);
        a.setStatus(status);
        audit.record(AuditAction.AGENT_STATUS_CHANGED, AuditEntityType.AGENT, a.getId(), status.name());
        return a;
    }

    /**
     * Links an agent to an AGENT account (D-030). Idempotent for the same pair; the rule order is
     * 404, 422 (non-AGENT account), idempotent, 409 on either side.
     */
    @Transactional
    public Agent linkUserAccount(UUID agentId, UUID userId) {
        Agent agent = get(agentId);
        AppUser user =
                users.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getRole() != AppRole.AGENT) {
            throw new BusinessRuleException(
                    "Only an AGENT account can be linked to an operational agent.");
        }
        AppUser current = agent.getUserAccount();
        if (current != null) {
            if (current.getId().equals(user.getId())) {
                return agent;
            }
            throw new ConflictException(
                    "Agent is already linked to account '"
                            + current.getUsername()
                            + "'. Unlink it first.");
        }
        Optional<Agent> other = agents.findByUserAccount(user);
        if (other.isPresent()) {
            throw new ConflictException(
                    "Account '"
                            + user.getUsername()
                            + "' is already linked to agent '"
                            + other.get().getDisplayName()
                            + "'.");
        }
        agent.linkUserAccount(user);
        agents.flush();
        audit.record(
                AuditAction.AGENT_ACCOUNT_LINKED,
                AuditEntityType.AGENT,
                agent.getId(),
                "linked to account " + user.getUsername());
        return agent;
    }

    /** Removes the link; idempotent. Neither the agent nor the account is deleted. */
    @Transactional
    public Agent unlinkUserAccount(UUID agentId) {
        Agent agent = get(agentId);
        AppUser previous = agent.getUserAccount();
        agent.unlinkUserAccount();
        if (previous != null) {
            audit.record(
                    AuditAction.AGENT_ACCOUNT_UNLINKED,
                    AuditEntityType.AGENT,
                    agent.getId(),
                    "unlinked from account " + previous.getUsername());
        }
        return agent;
    }

    @Transactional(readOnly = true)
    public Optional<Agent> findByUser(AppUser user) {
        return agents.findByUserAccount(user);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Agent> linkedAgentsByUserId() {
        Map<UUID, Agent> map = new HashMap<>();
        for (Agent a : agents.findByUserAccountIsNotNull()) {
            map.put(a.getUserAccount().getId(), a);
        }
        return map;
    }

    /** Agents that may be assigned to a new tour (US-17); S4 tour drafts use it. */
    @Transactional(readOnly = true)
    public List<Agent> findUsable() {
        return list(ResourceStatus.AVAILABLE);
    }
}
