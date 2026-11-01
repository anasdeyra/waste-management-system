package com.ase.wastems.service;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.AuditEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single hook for the audit log (US-39, D-029, D-049): mutating services call it after a
 * successful change inside their transaction, so a refused or rolled-back request leaves no
 * event.
 */
@Service
public class AuditService {

    public static final String SYSTEM_ACTOR = "system";
    private static final int BRIEF_MAX = 200;

    private final AuditEventRepository events;
    private final AppUserRepository users;

    public AuditService(AuditEventRepository events, AppUserRepository users) {
        this.events = events;
        this.users = users;
    }

    /** Records a SUCCESS event of the current actor (joins the caller's transaction). */
    @Transactional
    public AuditEvent record(
            AuditAction action, AuditEntityType entityType, Object entityId, String details) {
        return record(action, entityType, entityId, AuditOutcome.SUCCESS, details);
    }

    /** Same with an explicit outcome (used for ROUTING_FAILED runs). */
    @Transactional
    public AuditEvent record(
            AuditAction action,
            AuditEntityType entityType,
            Object entityId,
            AuditOutcome outcome,
            String details) {
        return events.save(
                new AuditEvent(
                        now(), currentActor(), action, entityType, idText(entityId), outcome,
                        cut(details, AuditEvent.DETAILS_MAX)));
    }

    /** Sign-in attempt: actor = attempted username; entity USER with the user's id when it exists. */
    @Transactional
    public AuditEvent recordSignIn(String attemptedUsername, boolean success, String details) {
        String actor = normalizeActor(attemptedUsername);
        String userId =
                users.findByUsername(actor).map(AppUser::getId).map(UUID::toString).orElse(null);
        return events.save(
                new AuditEvent(
                        now(),
                        actor,
                        AuditAction.SIGN_IN,
                        AuditEntityType.USER,
                        userId,
                        success ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE,
                        cut(details, AuditEvent.DETAILS_MAX)));
    }

    /** Sign-out of an authenticated user. */
    @Transactional
    public AuditEvent recordSignOut(String username) {
        String actor = normalizeActor(username);
        String userId =
                users.findByUsername(actor).map(AppUser::getId).map(UUID::toString).orElse(null);
        return events.save(
                new AuditEvent(
                        now(),
                        actor,
                        AuditAction.SIGN_OUT,
                        AuditEntityType.USER,
                        userId,
                        AuditOutcome.SUCCESS,
                        null));
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> search(AuditQuery q, int page, int size) {
        return events.search(
                q.from(),
                q.to(),
                q.actor(),
                q.action(),
                q.entityType(),
                q.entityId(),
                q.outcome(),
                PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public AuditEvent get(UUID id) {
        return events.findById(id)
                .orElseThrow(() -> new NotFoundException("Audit event not found: " + id));
    }

    /** Authenticated non-anonymous principal name, else "system". */
    public static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || auth instanceof AnonymousAuthenticationToken
                || !auth.isAuthenticated()) {
            return SYSTEM_ACTOR;
        }
        return normalizeActor(auth.getName());
    }

    /** Cuts free text to 200 characters (197 + "..."); null stays null. Used for reasons in details. */
    public static String brief(String text) {
        return text == null ? null : cut(text, BRIEF_MAX);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static String normalizeActor(String name) {
        if (name == null || name.isBlank()) {
            return SYSTEM_ACTOR;
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.length() > AuditEvent.ACTOR_MAX ? n.substring(0, AuditEvent.ACTOR_MAX) : n;
    }

    private static String idText(Object id) {
        if (id == null) {
            return null;
        }
        String s = String.valueOf(id);
        return s.length() > AuditEvent.ENTITY_ID_MAX ? s.substring(0, AuditEvent.ENTITY_ID_MAX) : s;
    }

    private static String cut(String text, int max) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }
}
