package com.ase.wastems.repository;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.AuditOutcome;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Append-only audit store: no update or delete method is exposed (US-39). */
public interface AuditEventRepository extends Repository<AuditEvent, UUID> {

    AuditEvent save(AuditEvent event);

    Optional<AuditEvent> findById(UUID id);

    long count();

    List<AuditEvent> findAll();

    @Query(
            value =
                    """
            select e from AuditEvent e
            where (:from is null or e.occurredAt >= :from)
              and (:to is null or e.occurredAt < :to)
              and (:actor is null or e.actor = :actor)
              and (:action is null or e.action = :action)
              and (:entityType is null or e.entityType = :entityType)
              and (:entityId is null or e.entityId = :entityId)
              and (:outcome is null or e.outcome = :outcome)
            order by e.occurredAt desc, e.id desc""",
            countQuery =
                    """
            select count(e) from AuditEvent e
            where (:from is null or e.occurredAt >= :from)
              and (:to is null or e.occurredAt < :to)
              and (:actor is null or e.actor = :actor)
              and (:action is null or e.action = :action)
              and (:entityType is null or e.entityType = :entityType)
              and (:entityId is null or e.entityId = :entityId)
              and (:outcome is null or e.outcome = :outcome)""")
    Page<AuditEvent> search(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("actor") String actor,
            @Param("action") AuditAction action,
            @Param("entityType") AuditEntityType entityType,
            @Param("entityId") String entityId,
            @Param("outcome") AuditOutcome outcome,
            Pageable pageable);
}
