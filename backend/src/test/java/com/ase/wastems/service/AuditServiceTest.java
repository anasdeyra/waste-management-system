package com.ase.wastems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.AuditEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/** Tests of {@link AuditService} and the append-only {@link AuditEventRepository} (US-39). */
@SpringBootTest
@ActiveProfiles("test")
class AuditServiceTest {

    @Autowired private AuditService audit;
    @Autowired private AuditEventRepository events;
    @Autowired private AppUserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from audit_event");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete from audit_event");
        SecurityContextHolder.clearContext();
        users.deleteAll();
    }

    @Test
    void recordUsesTheAuthenticatedActor() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "Planner1", null, List.of()));
        UUID id = UUID.randomUUID();
        audit.record(
                AuditAction.VEHICLE_CREATED, AuditEntityType.VEHICLE, id, "EV-1, capacity 1000 kg");
        AuditEvent e = events.findAll().get(0);
        assertEquals("planner1", e.getActor());
        assertEquals(AuditOutcome.SUCCESS, e.getOutcome());
        assertEquals(AuditEntityType.VEHICLE, e.getEntityType());
        assertEquals(id.toString(), e.getEntityId());
        assertEquals("EV-1, capacity 1000 kg", e.getDetails());
        assertTrue(Duration.between(e.getOccurredAt(), Instant.now()).abs().getSeconds() < 5);
        assertEquals(e.getOccurredAt(), e.getOccurredAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void recordWithoutAuthenticationUsesSystem() {
        audit.record(AuditAction.DEMO_LOADED, AuditEntityType.DEMO_DATA, null, "x");
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new AnonymousAuthenticationToken(
                                "k", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        audit.record(AuditAction.DEMO_LOADED, AuditEntityType.DEMO_DATA, null, "y");
        List<AuditEvent> all = events.findAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().allMatch(e -> e.getActor().equals("system")));
        assertTrue(all.stream().allMatch(e -> e.getEntityId() == null));
    }

    @Test
    void longValuesAreCutNotRejected() {
        audit.record(
                AuditAction.TOUR_CANCELLED, AuditEntityType.TOUR, "i".repeat(100), "d".repeat(600));
        AuditEvent e = events.findAll().get(0);
        assertEquals(500, e.getDetails().length());
        assertTrue(e.getDetails().endsWith("..."));
        assertEquals(80, e.getEntityId().length());
        String brief = AuditService.brief("r".repeat(250));
        assertEquals(200, brief.length());
        assertTrue(brief.endsWith("..."));
        assertNull(AuditService.brief(null));
    }

    @Test
    void signInFailureKeepsAttemptedUsernameOnly() {
        AppUser known = users.save(new AppUser("known1", "x", AppRole.PLANNER));
        audit.recordSignIn("  Known1 ", false, "invalid credentials");
        audit.recordSignIn("ghost", false, "invalid credentials");
        List<AuditEvent> all = events.findAll();
        AuditEvent k = all.stream().filter(e -> e.getActor().equals("known1")).findFirst().orElseThrow();
        assertEquals(AuditOutcome.FAILURE, k.getOutcome());
        assertEquals(AuditEntityType.USER, k.getEntityType());
        assertEquals(known.getId().toString(), k.getEntityId());
        AuditEvent g = all.stream().filter(e -> e.getActor().equals("ghost")).findFirst().orElseThrow();
        assertNull(g.getEntityId());
        assertEquals(AuditAction.SIGN_IN, g.getAction());
    }

    @Test
    void searchFiltersAndOrdersNewestFirst() {
        Instant t0 = Instant.parse("2026-10-01T08:00:00Z");
        events.save(new AuditEvent(t0, "a", AuditAction.VEHICLE_CREATED, AuditEntityType.VEHICLE, "v1", AuditOutcome.SUCCESS, null));
        events.save(new AuditEvent(t0.plusMillis(1), "b", AuditAction.SIGN_IN, AuditEntityType.USER, null, AuditOutcome.FAILURE, null));
        events.save(new AuditEvent(t0.plusMillis(2), "a", AuditAction.TOUR_CANCELLED, AuditEntityType.TOUR, "t1", AuditOutcome.SUCCESS, null));
        events.save(new AuditEvent(t0.plusMillis(3), "c", AuditAction.TOUR_STARTED, AuditEntityType.TOUR, "t1", AuditOutcome.SUCCESS, null));
        events.save(new AuditEvent(t0.plusMillis(4), "b", AuditAction.SIGN_IN, AuditEntityType.USER, null, AuditOutcome.SUCCESS, null));
        events.save(new AuditEvent(t0.plusMillis(5), "a", AuditAction.USER_CREATED, AuditEntityType.USER, "u1", AuditOutcome.SUCCESS, null));

        Page<AuditEvent> all = audit.search(new AuditQuery(null, null, null, null, null, null, null), 0, 50);
        assertEquals(6, all.getTotalElements());
        assertEquals(AuditAction.USER_CREATED, all.getContent().get(0).getAction());
        assertEquals(AuditAction.VEHICLE_CREATED, all.getContent().get(5).getAction());

        assertEquals(3, audit.search(new AuditQuery(null, null, "a", null, null, null, null), 0, 50).getTotalElements());
        assertEquals(1, audit.search(new AuditQuery(null, null, null, AuditAction.TOUR_STARTED, null, null, null), 0, 50).getTotalElements());
        assertEquals(2, audit.search(new AuditQuery(null, null, null, null, AuditEntityType.TOUR, "t1", null), 0, 50).getTotalElements());
        assertEquals(1, audit.search(new AuditQuery(null, null, null, null, null, null, AuditOutcome.FAILURE), 0, 50).getTotalElements());
        // from inclusive, to exclusive
        Page<AuditEvent> window =
                audit.search(new AuditQuery(t0.plusMillis(1), t0.plusMillis(3), null, null, null, null, null), 0, 50);
        assertEquals(2, window.getTotalElements());
        Page<AuditEvent> paged = audit.search(new AuditQuery(null, null, null, null, null, null, null), 1, 2);
        assertEquals(2, paged.getContent().size());
        assertEquals(6, paged.getTotalElements());
        assertEquals(3, paged.getTotalPages());
    }

    @Test
    void getUnknownEventIs404() {
        assertThrows(NotFoundException.class, () -> audit.get(UUID.randomUUID()));
    }

    @Test
    void repositoryExposesNoUpdateOrDelete() {
        assertTrue(
                Arrays.stream(AuditEventRepository.class.getMethods())
                        .noneMatch(m -> m.getName().startsWith("delete")));
        assertFalse(CrudRepository.class.isAssignableFrom(AuditEventRepository.class));
    }
}
