package com.ase.wastems.web.dto;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.AuditOutcome;
import java.time.Instant;
import java.util.UUID;

/** One audit-log entry (US-39). */
public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        String actor,
        AuditAction action,
        AuditEntityType entityType,
        String entityId,
        AuditOutcome outcome,
        String details) {

    public static AuditEventResponse from(AuditEvent e) {
        return new AuditEventResponse(
                e.getId(),
                e.getOccurredAt(),
                e.getActor(),
                e.getAction(),
                e.getEntityType(),
                e.getEntityId(),
                e.getOutcome(),
                e.getDetails());
    }
}
