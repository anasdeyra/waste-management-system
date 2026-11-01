package com.ase.wastems.web;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.service.AuditQuery;
import com.ase.wastems.service.AuditService;
import com.ase.wastems.service.InvalidRequestException;
import com.ase.wastems.web.dto.AuditEventPageResponse;
import com.ase.wastems.web.dto.AuditEventResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only audit log for administrators (US-39, D-029): no endpoint creates, edits or deletes an
 * event. ADMIN only through the {@code /api/admin/**} rule of SecurityConfig. Reading the log is
 * not audited.
 */
@RestController
@RequestMapping("/api/admin/audit-events")
public class AuditEventController {

    private final AuditService audit;

    public AuditEventController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public AuditEventPageResponse search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0) {
            throw new InvalidRequestException("page", "must be zero or more");
        }
        if (size < 1 || size > 200) {
            throw new InvalidRequestException("size", "must be between 1 and 200");
        }
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidRequestException("to", "must be after from");
        }
        String normalizedActor = actor == null || actor.isBlank() ? null : actor.trim().toLowerCase(Locale.ROOT);
        String normalizedEntityId = entityId == null || entityId.isBlank() ? null : entityId.trim();
        return AuditEventPageResponse.from(
                audit.search(
                        new AuditQuery(from, to, normalizedActor, action, entityType, normalizedEntityId, outcome),
                        page,
                        size));
    }

    @GetMapping("/{id}")
    public AuditEventResponse get(@PathVariable UUID id) {
        return AuditEventResponse.from(audit.get(id));
    }
}
