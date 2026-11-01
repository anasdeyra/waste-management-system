package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Identity of one demonstration record (US-33): makes the demo load idempotent. Provenance
 * outside the D-033 class diagram, like SourceSnapshot.
 */
@Entity
@Table(name = "demo_record")
public class DemoRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "demo_key", nullable = false, unique = true, length = 60)
    private String demoKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private DemoEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DemoRecord() {}

    public DemoRecord(String demoKey, DemoEntityType entityType, UUID entityId, Instant createdAt) {
        this.demoKey = demoKey;
        this.entityType = entityType;
        this.entityId = entityId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getDemoKey() {
        return demoKey;
    }

    public DemoEntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
