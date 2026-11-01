package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import java.util.UUID;

/** An agent as seen by the client; linkedUser is omitted when there is no link. */
public record AgentResponse(
        UUID id,
        String displayName,
        ResourceStatus status,
        DataOrigin origin,
        LinkedUserResponse linkedUser) {

    public static AgentResponse from(Agent a) {
        return new AgentResponse(
                a.getId(),
                a.getDisplayName(),
                a.getStatus(),
                a.getOrigin(),
                a.getUserAccount() == null ? null : LinkedUserResponse.from(a.getUserAccount()));
    }
}
