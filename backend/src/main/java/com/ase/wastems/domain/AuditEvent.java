package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One immutable audit-log entry (US-39, D-029): actor or attempted username, UTC time, action,
 * entity, outcome and concise details. Never contains passwords, hashes or tokens. Outside the
 * D-033 class diagram, like SourceSnapshot.
 */
@Entity
@org.hibernate.annotations.Immutable
@Table(
        name = "audit_event",
        indexes = @Index(name = "ix_audit_event_occurred_at", columnList = "occurred_at"))
public class AuditEvent {

    public static final int ACTOR_MAX = 50;
    public static final int ENTITY_ID_MAX = 80;
    public static final int DETAILS_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 50)
    private String actor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entity_type", length = 30)
    private AuditEntityType entityType;

    @Column(name = "entity_id", length = 80)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditOutcome outcome;

    @Column(length = 500)
    private String details;

    protected AuditEvent() {}

    public AuditEvent(
            Instant occurredAt,
            String actor,
            AuditAction action,
            AuditEntityType entityType,
            String entityId,
            AuditOutcome outcome,
            String details) {
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.actor = Objects.requireNonNull(actor);
        this.action = Objects.requireNonNull(action);
        this.entityType = entityType;
        this.entityId = entityId;
        this.outcome = Objects.requireNonNull(outcome);
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getDetails() {
        return details;
    }
}
