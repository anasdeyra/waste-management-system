package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Agent;

/** Read model of a collection agent returned by the REST API. */
public record AgentResponse(Long id, String fullName, String phone, String role, String status) {

    public static AgentResponse from(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getFullName(),
                agent.getPhone(),
                agent.getRole().name(),
                agent.getStatus().name());
    }
}
