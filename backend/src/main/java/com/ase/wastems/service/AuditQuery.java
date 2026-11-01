package com.ase.wastems.service;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditOutcome;
import java.time.Instant;

/** Filters of an audit-log search (US-39); a null component means no filter. */
public record AuditQuery(
        Instant from,
        Instant to,
        String actor,
        AuditAction action,
        AuditEntityType entityType,
        String entityId,
        AuditOutcome outcome) {}
