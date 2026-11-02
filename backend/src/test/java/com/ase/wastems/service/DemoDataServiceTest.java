package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.CollectedQuantity;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemoEntityType;
import com.ase.wastems.domain.DemoRecord;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.domain.ProposalDecision;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.StopDemand;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.TourStopStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AuditEventRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DemoRecordRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** The reproducible MN01 demonstration on the real snapshots (S7, US-33, D-055 reset). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "wastems.demo.enabled=true")
class DemoDataServiceTest {

    @Autowired private DemoDataService demo;
    @Autowired private Mn01ImportService mn01Import;
    @Autowired private TourService tourService;
    @Autowired private EvaluationService evaluationService;
    @Autowired private TourExecutionService executionService;
    @Autowired private DemoRecordRepository demoRecords;
    @Autowired private EvaluationRunRepository evaluationRuns;
    @Autowired private TourRepository tours;
    @Autowired private AgentRepository agents;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private VehicleRepository vehicles;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private AppUserRepository users;
    @Autowired private SourceSnapshotRepository sourceSnapshots;
    @Autowired private PasswordEncoder encoder;
    @Autowired private MockMvc mvc;
    @Autowired private AuditEventRepository auditEvents;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        wipe();
        users.save(new AppUser("agent", encoder.encode("agent123"), AppRole.AGENT));
        mn01Import.importAll(ImportTrigger.MANUAL);
    }

    @AfterEach
    void tearDown() {
        wipe();
    }

    private void wipe() {
        jdbc.update("delete from audit_event");
        demoRecords.deleteAll();
        evaluationRuns.deleteAll();
        tours.deleteAll();
        agents.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        vehicles.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        sourceSnapshots.deleteAll();
    }

    private Tour tourOf(DemoLoadReport r, String key) {
        UUID id = r.tourStates().stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow().tourId();
        return tourService.get(id);
    }

    private Map<String, TourStop> stopsBySource(Tour t) {
        return t.getStops().stream().collect(Collectors.toMap(
                s -> s.getCollectionPoint().getSourceIdentifier(), s -> s));
    }

    private Agent agentBy(String startsWith) {
        return agents.findAll().stream().filter(a -> a.getDisplayName().startsWith(startsWith))
                .findFirst().orElseThrow();
    }

    private static String bd(BigDecimal v) {
        return v.setScale(2).toPlainString();
    }

    @Test
    void loadIsAuditedWithoutPasswords() {
        jdbc.update("delete from audit_event");
        demo.load(DemoTrigger.MANUAL);
        List<AuditEvent> all = auditEvents.findAll();
        List<AuditEvent> loaded = all.stream().filter(e -> e.getAction() == AuditAction.DEMO_LOADED).toList();
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getActor()).isEqualTo("system");
        assertThat(loaded.get(0).getDetails()).startsWith("MANUAL:").contains("tours 6");
        for (AuditAction a : List.of(
                AuditAction.TOUR_CONFIRMED, AuditAction.TOUR_COMPLETED, AuditAction.TOUR_CANCELLED)) {
            List<AuditEvent> steps = all.stream().filter(e -> e.getAction() == a).toList();
            assertThat(steps).isNotEmpty();
            assertThat(steps).allMatch(e -> e.getActor().equals("system"));
        }
        for (AuditEvent e : all) {
            String text = e.getActor() + " " + e.getDetails();
            assertThat(text).doesNotContain("agent123", "collector123", "agent2123", "$2a$");
        }
    }

    @Test
    void firstLoadCreatesEveryLifecycleState() {
        DemoLoadReport r = demo.load(DemoTrigger.MANUAL);
        assertThat(r.categories()).isEqualTo(new DemoCount(2, 1));
        assertThat(r.demandEntries()).isEqualTo(new DemoCount(30, 0));
        assertThat(r.vehicles()).isEqualTo(new DemoCount(5, 0));
        assertThat(r.accounts()).isEqualTo(new DemoCount(2, 1));
        assertThat(r.agents()).isEqualTo(new DemoCount(7, 0));
        assertThat(r.tours()).isEqualTo(new DemoCount(6, 0));
        assertThat(r.warnings()).isEmpty();
        assertThat(r.tourStates().stream().map(s -> s.status()).toList()).containsExactly(
                TourStatus.DRAFT, TourStatus.PLANNED, TourStatus.PLANNED, TourStatus.IN_PROGRESS,
                TourStatus.COMPLETED, TourStatus.CANCELLED);

        assertThat(vehicles.findAll()).hasSize(5).allMatch(v -> v.getOrigin() == DataOrigin.SYNTHETIC);
        assertThat(vehicles.findByRegistration("DEMO-03").orElseThrow().getStatus()).isEqualTo(ResourceStatus.UNAVAILABLE);
        List<Agent> all = agents.findAll();
        assertThat(all).hasSize(7).allMatch(a -> a.getOrigin() == DataOrigin.SYNTHETIC);
        assertThat(agentBy("Alex Rivera").getUserAccount().getUsername()).isEqualTo("agent");
        assertThat(agentBy("Blair Chen").getUserAccount().getUsername()).isEqualTo("collector");
        assertThat(agentBy("Casey Morgan").getUserAccount().getUsername()).isEqualTo("agent2");
        for (String name : List.of("Dana", "Farah", "Grant", "Eli")) {
            assertThat(agentBy(name).getUserAccount()).isNull();
        }
        assertThat(agentBy("Eli").getStatus()).isEqualTo(ResourceStatus.UNAVAILABLE);

        Tour draft = tourOf(r, "tour:draft");
        assertThat(draft.getOrigin()).isEqualTo(DataOrigin.SYNTHETIC);
        assertThat(draft.getStops()).hasSize(12);
        assertThat(draft.hasRoute()).isFalse();
        List<EvaluationRun> draftRuns = evaluationService.listForTour(draft.getId());
        assertThat(draftRuns).hasSize(1);
        EvaluationRun run = draftRuns.get(0);
        assertThat(run.getDecision()).isNull();
        Map<PlanningMethod, Long> meters = run.getResults().stream().collect(
                Collectors.toMap(MethodResult::getMethod, m -> Math.round(m.getDistanceM())));
        // Distances of the 12-stop Tribeca draft on the versioned MN01 road snapshot (D-055).
        assertThat(meters).containsEntry(PlanningMethod.INPUT_ORDER, 8740L)
                .containsEntry(PlanningMethod.SWEEP, 7921L)
                .containsEntry(PlanningMethod.NEAREST_NEIGHBOR, 4643L)
                .containsEntry(PlanningMethod.NN_PLUS_2_OPT, 4643L);

        Tour planned = tourOf(r, "tour:planned");
        assertThat(planned.getStatus()).isEqualTo(TourStatus.PLANNED);
        assertThat(planned.hasRoute()).isTrue();
        assertThat(planned.getRouteMethod()).isEqualTo(PlanningMethod.NN_PLUS_2_OPT);
        assertThat(Math.round(planned.getRouteDistanceM())).isEqualTo(4962L);
        assertThat(planned.getStops()).hasSize(8).allMatch(s -> s.getStatus() == TourStopStatus.PENDING);
        assertThat(planned.getVehicle().getRegistration()).isEqualTo("DEMO-02");
        assertThat(planned.getDriver().getDisplayName()).startsWith("Casey");
        assertThat(planned.getCollectors()).extracting(Agent::getDisplayName).containsExactly("Dana Lopez (demo)");
        EvaluationRun accepted = evaluationRuns.findById(planned.getRouteEvaluationRunId()).orElseThrow();
        assertThat(accepted.getDecision()).isEqualTo(ProposalDecision.ACCEPTED);
        assertThat(accepted.getDecidedBy()).isEqualTo("demo-loader");
        TourStop civicCenter = stopsBySource(planned).get("10130067");
        assertThat(civicCenter.getStopDemands().stream().collect(Collectors.toMap(
                d -> d.getCategory().getName(), d -> bd(d.getPlannedQuantityKg()))))
                .containsEntry("Mixed litter", "22.00")
                .containsEntry("Recyclables", "12.00")
                .containsEntry("Organics", "6.50");

        Tour planned2 = tourOf(r, "tour:planned-2");
        assertThat(planned2.getStatus()).isEqualTo(TourStatus.PLANNED);
        assertThat(planned2.hasRoute()).isTrue();
        assertThat(planned2.getStops()).hasSize(6).allMatch(s -> s.getStatus() == TourStopStatus.PENDING);
        assertThat(planned2.getVehicle().getRegistration()).isEqualTo("DEMO-01");
        assertThat(planned2.getDriver().getDisplayName()).startsWith("Farah");

        Tour running = tourOf(r, "tour:in-progress");
        assertThat(running.getStatus()).isEqualTo(TourStatus.IN_PROGRESS);
        assertThat(running.getStartedAt()).isNotNull();
        assertThat(running.getStartedBy()).isEqualTo("agent");
        assertThat(running.getStops()).hasSize(7);
        List<TourStop> recorded = running.getStops().stream()
                .filter(s -> s.getVisitOrder() <= 3).toList();
        assertThat(recorded).hasSize(3);
        assertThat(recorded.get(0).getStatus()).isEqualTo(TourStopStatus.COLLECTED);
        assertThat(recorded.get(1).getStatus()).isEqualTo(TourStopStatus.COLLECTED);
        assertThat(recorded.get(2).getStatus()).isEqualTo(TourStopStatus.NOT_COLLECTED);
        assertThat(recorded.get(2).getIncidentReason()).isEqualTo(
                "Basket blocked by a parked delivery truck (synthetic demo outcome).");
        assertThat(running.getStops().stream().filter(s -> s.getVisitOrder() > 3))
                .hasSize(4).allMatch(s -> s.getStatus() == TourStopStatus.PENDING);
        for (int order = 1; order <= 2; order++) {
            TourStop s = recorded.get(order - 1);
            BigDecimal factor = factors(order);
            Map<String, String> actuals = s.getActualQuantities().stream().collect(
                    Collectors.toMap(q -> q.getCategory().getName(),
                            q -> bd(q.getActualQuantityKg())));
            for (StopDemand d : s.getStopDemands()) {
                assertThat(actuals).containsEntry(d.getCategory().getName(),
                        bd(d.getPlannedQuantityKg().multiply(factor).setScale(2, RoundingMode.HALF_UP)));
            }
        }

        Tour done = tourOf(r, "tour:completed");
        assertThat(done.getStatus()).isEqualTo(TourStatus.COMPLETED);
        assertThat(done.getCompletedAt()).isNotNull();
        assertThat(done.getCompletedBy()).isEqualTo("collector");
        List<TourStop> collected = done.getStops().stream()
                .filter(s -> s.getStatus() == TourStopStatus.COLLECTED).toList();
        assertThat(collected).hasSize(8);
        TourStop notCollected = done.getStops().stream()
                .filter(s -> s.getStatus() == TourStopStatus.NOT_COLLECTED).findFirst().orElseThrow();
        assertThat(notCollected.getVisitOrder()).isEqualTo(5);
        assertThat(notCollected.getIncidentReason()).isEqualTo(
                "Street works prevented access to the basket (synthetic demo outcome).");
        assertThat(stopsBySource(done).get("10130202").getStatus()).isEqualTo(TourStopStatus.NOT_COLLECTED);
        assertThat(stopsBySource(done).get("10130305").getStatus()).isEqualTo(TourStopStatus.COLLECTED);
        assertThat(stopsBySource(done).get("10130337").getStatus()).isEqualTo(TourStopStatus.COLLECTED);
        // US-33 closes US-31: the completed tour carries one retained, privileged correction.
        List<TourStop> corrected = collected.stream().filter(s -> !s.getCorrections().isEmpty()).toList();
        assertThat(corrected).hasSize(1);
        TourStop c = corrected.get(0);
        assertThat(c.getVisitOrder()).isEqualTo(4);
        assertThat(c.getCorrections().get(0).getReason()).contains("(synthetic demo correction)");
        assertThat(c.getCorrections().get(0).getCorrectedBy()).isEqualTo("planner");
        Map<String, String> actualNow = c.getActualQuantities().stream().collect(
                Collectors.toMap(q -> q.getCategory().getName(), q -> bd(q.getActualQuantityKg())));
        for (StopDemand d : c.getStopDemands()) {
            assertThat(actualNow).containsEntry(d.getCategory().getName(),
                    bd(d.getPlannedQuantityKg().multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP)));
        }
        assertThat(done.correctionCount()).isEqualTo(1);

        Tour cancelled = tourOf(r, "tour:cancelled");
        assertThat(cancelled.getStatus()).isEqualTo(TourStatus.CANCELLED);
        assertThat(cancelled.getCancelledFromStatus()).isEqualTo(TourStatus.PLANNED);
        assertThat(cancelled.getCancelledBy()).isEqualTo("demo-loader");
        assertThat(cancelled.getCancellationReason()).contains("route postponed one day");
        assertThat(cancelled.hasRoute()).isTrue();
        assertThat(cancelled.getStops()).hasSize(6).allMatch(s -> s.getStatus() == TourStopStatus.PENDING);
    }

    private BigDecimal factors(int visitOrder) {
        BigDecimal[] fs = {
            new BigDecimal("1.00"), new BigDecimal("0.94"), new BigDecimal("0.86"),
            new BigDecimal("0.78"), new BigDecimal("0.69"), new BigDecimal("0.91")
        };
        return fs[(visitOrder - 1) % fs.length];
    }

    @Test
    void secondLoadIsIdempotent() {
        DemoLoadReport first = demo.load(DemoTrigger.MANUAL);
        long runsBefore = evaluationRuns.count();
        DemoLoadReport second = demo.load(DemoTrigger.MANUAL);
        assertThat(second.categories()).isEqualTo(new DemoCount(0, 3));
        assertThat(second.demandEntries()).isEqualTo(new DemoCount(0, 30));
        assertThat(second.vehicles()).isEqualTo(new DemoCount(0, 5));
        assertThat(second.accounts()).isEqualTo(new DemoCount(0, 3));
        assertThat(second.agents()).isEqualTo(new DemoCount(0, 7));
        assertThat(second.tours()).isEqualTo(new DemoCount(0, 6));
        assertThat(second.warnings()).isEmpty();
        assertThat(second.tourStates()).isEqualTo(first.tourStates().stream().map(s -> s).toList());
        assertThat(demoRecords.count()).isEqualTo(54);
        assertThat(tours.count()).isEqualTo(6);
        assertThat(vehicles.count()).isEqualTo(5);
        assertThat(users.count()).isEqualTo(3);
        assertThat(agents.count()).isEqualTo(7);
        assertThat(categories.count()).isEqualTo(3);
        assertThat(evaluationRuns.count()).isEqualTo(runsBefore);
        assertThat(stopsBySource(tourOf(first, "tour:planned")).get("10130067").getStopDemands()).hasSize(3);
    }

    @Test
    void loadDoesNotResetReviewerProgress() {
        DemoLoadReport r = demo.load(DemoTrigger.MANUAL);
        Tour planned = tourOf(r, "tour:planned");
        executionService.start(planned.getId(), "agent2");
        TourStop stop = tourService.get(planned.getId()).getStops().get(0);
        executionService.recordOutcome(planned.getId(), stop.getId(), "agent2",
                new StopOutcomeCommand(TourStopStatus.NOT_COLLECTED, List.of(), "Reviewer test"));
        Vehicle v = vehicles.findByRegistration("DEMO-01").orElseThrow();
        v.setStatus(ResourceStatus.UNAVAILABLE);
        vehicles.save(v);
        Agent blair = agentBy("Blair");
        blair.unlinkUserAccount();
        agents.save(blair);

        DemoLoadReport again = demo.load(DemoTrigger.MANUAL);
        Tour after = tourService.get(planned.getId());
        assertThat(after.getStatus()).isEqualTo(TourStatus.IN_PROGRESS);
        assertThat(after.getStops().get(0).getStatus()).isEqualTo(TourStopStatus.NOT_COLLECTED);
        assertThat(vehicles.findByRegistration("DEMO-01").orElseThrow().getStatus()).isEqualTo(ResourceStatus.UNAVAILABLE);
        assertThat(agents.findById(blair.getId()).orElseThrow().getUserAccount()).isNull();
        assertThat(again.tours()).isEqualTo(new DemoCount(0, 6));
        assertThat(tours.count()).isEqualTo(6);
    }

    @Test
    void loadToleratesExistingDevData() {
        vehicles.save(new Vehicle("DEMO-01", new BigDecimal("900"), DataOrigin.USER_ENTERED));
        Agent ann = new Agent("Owner Ann", DataOrigin.USER_ENTERED);
        ann.linkUserAccount(users.findByUsername("agent").orElseThrow());
        ann = agents.save(ann);
        users.save(new AppUser("agent2", "x", AppRole.PLANNER));
        CollectionPoint inactive = points.findBySourceDataset("8znf-7b2c").stream()
                .filter(p -> "10130005".equals(p.getSourceIdentifier())).findFirst().orElseThrow();
        inactive.setStatus(CollectionPointStatus.INACTIVE);
        points.save(inactive);
        WasteCategory rec = new WasteCategory("Recyclables", null);
        rec.setActive(false);
        categories.save(rec);

        DemoLoadReport r = demo.load(DemoTrigger.MANUAL);
        assertThat(r.vehicles()).isEqualTo(new DemoCount(4, 1));
        assertThat(r.accounts()).isEqualTo(new DemoCount(1, 2));
        assertThat(r.tours()).isEqualTo(new DemoCount(6, 0));
        String warnings = String.join("\n", r.warnings());
        assertThat(warnings).contains("DEMO-01", "'agent'", "'agent2'", "10130005", "Recyclables");
        assertThat(r.warnings().stream().filter(w -> w.contains("no crew member with a linked active AGENT account")))
                .hasSize(1);
        assertThat(r.tourStates().stream().map(s -> s.status()).toList()).containsExactly(
                TourStatus.DRAFT, TourStatus.PLANNED, TourStatus.PLANNED, TourStatus.PLANNED,
                TourStatus.COMPLETED, TourStatus.CANCELLED);
        assertThat(tourOf(r, "tour:planned").getStops()).hasSize(8);
        assertThat(tourOf(r, "tour:cancelled").getStops()).hasSize(5);
        assertThat(agents.findById(ann.getId()).orElseThrow().getUserAccount().getUsername()).isEqualTo("agent");

        DemoLoadReport second = demo.load(DemoTrigger.MANUAL);
        assertThat(second.tours().created()).isZero();
        assertThat(tours.count()).isEqualTo(6);
    }

    @Test
    void missingImportIsRefused() {
        points.deleteAll();
        depots.deleteAll();
        assertThatThrownBy(() -> demo.load(DemoTrigger.MANUAL)).isInstanceOf(BusinessRuleException.class);
        assertThat(demoRecords.count()).isZero();
    }

    @Test
    void resetRebuildsTheDemonstrationAndKeepsAccountsAndImport() {
        demo.load(DemoTrigger.MANUAL);
        UUID agentAccountId = users.findByUsername("agent").orElseThrow().getId();
        UUID pointId = points
                .findBySourceDatasetAndSourceIdentifier("8znf-7b2c", "10130002").orElseThrow().getId();
        long demandBefore = jdbc.queryForObject("select count(*) from demand_entry", Long.class);
        long pointsBefore = points.count();
        long depotCount = depots.count();
        long runCount = evaluationRuns.count();

        DemoLoadReport r = demo.reset(DemoTrigger.RESET);

        assertThat(r.trigger()).isEqualTo(DemoTrigger.RESET);
        assertThat(r.demandEntries()).isEqualTo(new DemoCount(30, 0));
        assertThat(r.vehicles()).isEqualTo(new DemoCount(5, 0));
        assertThat(r.agents()).isEqualTo(new DemoCount(7, 0));
        assertThat(r.tours()).isEqualTo(new DemoCount(6, 0));
        assertThat(r.accounts()).isEqualTo(new DemoCount(0, 3));
        assertThat(r.categories()).isEqualTo(new DemoCount(0, 3));
        assertThat(r.warnings()).isEmpty();

        // user accounts are kept with their identities and links
        assertThat(users.count()).isEqualTo(3);
        assertThat(users.findByUsername("agent").orElseThrow().getId()).isEqualTo(agentAccountId);
        assertThat(agentBy("Alex").getUserAccount().getUsername()).isEqualTo("agent");
        assertThat(agentBy("Blair").getUserAccount().getUsername()).isEqualTo("collector");
        assertThat(agentBy("Casey").getUserAccount().getUsername()).isEqualTo("agent2");
        // the imported MN01 data is kept
        assertThat(points.count()).isEqualTo(pointsBefore);
        assertThat(depots.count()).isEqualTo(depotCount);
        assertThat(points.findBySourceDatasetAndSourceIdentifier("8znf-7b2c", "10130002").orElseThrow().getId())
                .isEqualTo(pointId);
        assertThat(jdbc.queryForObject("select count(*) from demand_entry", Long.class)).isEqualTo(demandBefore);
        assertThat(evaluationRuns.count()).isGreaterThanOrEqualTo(runCount);
        // the demonstration is rebuilt from scratch on fresh identities
        assertThat(tours.count()).isEqualTo(6);
        assertThat(vehicles.count()).isEqualTo(5);
        assertThat(agents.count()).isEqualTo(7);
        assertThat(tours.findByOrigin(DataOrigin.SYNTHETIC))
                .hasSize(6)
                .allMatch(t -> t.getId() != null);
        Map<DemoEntityType, Long> records = demoRecords.findAll().stream()
                .collect(Collectors.groupingBy(DemoRecord::getEntityType, Collectors.counting()));
        assertThat(records)
                .containsEntry(DemoEntityType.TOUR, 6L)
                .containsEntry(DemoEntityType.AGENT, 7L)
                .containsEntry(DemoEntityType.VEHICLE, 5L)
                .containsEntry(DemoEntityType.DEMAND_ENTRY, 30L)
                .containsEntry(DemoEntityType.ACCOUNT, 3L)
                .containsEntry(DemoEntityType.CATEGORY, 3L);
        List<AuditEvent> resets = auditEvents.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.DEMO_RESET).toList();
        assertThat(resets).hasSize(1);
        assertThat(resets.get(0).getDetails())
                .startsWith("RESET:").contains("tours 6", "demand entries 30", "vehicles 5", "agents 7");
        // the rebuilt portfolio behaves like a fresh load: one correction, one cancelled tour
        assertThat(tourOf(r, "tour:completed").correctionCount()).isEqualTo(1);
        assertThat(tourOf(r, "tour:cancelled").getStatus()).isEqualTo(TourStatus.CANCELLED);
    }

    @Test
    void resetRefusedWithoutImportDeletesNothing() {
        demo.load(DemoTrigger.MANUAL);
        tours.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        assertThatThrownBy(() -> demo.reset(DemoTrigger.RESET)).isInstanceOf(BusinessRuleException.class);
        assertThat(tours.count()).isZero();
        assertThat(vehicles.count()).isEqualTo(5);
        assertThat(agents.count()).isEqualTo(7);
        assertThat(demoRecords.count()).isEqualTo(54);
    }

    @Test
    void resetRefusedWhileUserToursUseDemoResources() {
        demo.load(DemoTrigger.MANUAL);
        Depot depot = depots.findBySourceDatasetAndSourceIdentifier("xw3j-2yxf", "MN01G").orElseThrow();
        CollectionPoint p = points
                .findBySourceDatasetAndSourceIdentifier("8znf-7b2c", "10110006").orElseThrow();
        Tour user = tourService.create(LocalDate.now(), depot.getId());
        tourService.addStop(user.getId(), p.getId());
        tourService.assign(user.getId(),
                vehicles.findByRegistration("DEMO-01").orElseThrow().getId(),
                agentBy("Alex").getId(), List.of());

        assertThatThrownBy(() -> demo.reset(DemoTrigger.RESET))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("still use demo crew or demo vehicles");

        // nothing was deleted: the user tour, the demo tours and all demo resources stay
        Tour kept = tourService.get(user.getId());
        assertThat(kept.getOrigin()).isEqualTo(DataOrigin.USER_ENTERED);
        assertThat(kept.getDriver().getDisplayName()).startsWith("Alex");
        assertThat(kept.getVehicle().getRegistration()).isEqualTo("DEMO-01");
        assertThat(tours.count()).isEqualTo(7);
        assertThat(vehicles.count()).isEqualTo(5);
        assertThat(agents.count()).isEqualTo(7);
        assertThat(demoRecords.count()).isEqualTo(54);
    }

    @Test
    void demoEndpointRoleMatrix() throws Exception {
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        for (String path : List.of("/api/admin/demo", "/api/admin/demo/reset")) {
            mvc.perform(post(path).with(user("agent1").roles("AGENT")).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).with(user("planner1").roles("PLANNER")).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/admin/demo").with(user("admin1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tours.created").value(6));
        mvc.perform(get("/api/admin/demo/report").with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("MANUAL"));
        mvc.perform(post("/api/admin/demo/reset").with(user("admin1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("RESET"))
                .andExpect(jsonPath("$.tours.created").value(6));
        mvc.perform(get("/api/admin/demo/report").with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("RESET"));
    }

    @Test
    void dashboardSummaryMatchesTheDemonstration() {
        demo.load(DemoTrigger.MANUAL);
        OperationsSummary s = dashboardService.operations(null, null);
        assertThat(s.tours().total()).isEqualTo(6);
        assertThat(s.tours().draft()).isEqualTo(1);
        assertThat(s.tours().planned()).isEqualTo(2);
        assertThat(s.tours().inProgress()).isEqualTo(1);
        assertThat(s.tours().completed()).isEqualTo(1);
        assertThat(s.tours().cancelled()).isEqualTo(1);
        // PLANNED (8 + 6 stops) + IN_PROGRESS and COMPLETED tours; drafts and the cancelled tour
        // are not committed work.
        assertThat(s.stops().total()).isEqualTo(30);
        assertThat(s.stops().pending()).isEqualTo(18);
        assertThat(s.stops().collected()).isEqualTo(10);
        assertThat(s.stops().notCollected()).isEqualTo(2);
        assertThat(s.vehicles().total()).isEqualTo(vehicles.count());
        assertThat(s.vehicles().unavailable()).isEqualTo(1);
        assertThat(s.agents().total()).isEqualTo(agents.count());
        assertThat(s.agents().unavailable()).isEqualTo(1);
        BigDecimal expected = BigDecimal.ZERO;
        for (Tour t : tours.findAll()) {
            Tour full = tourService.get(t.getId());
            if (full.getStatus() == TourStatus.DRAFT || full.getStatus() == TourStatus.CANCELLED) {
                continue;
            }
            for (TourStop stop : full.getStops()) {
                if (stop.getStatus() == TourStopStatus.COLLECTED) {
                    expected = expected.add(stop.actualTotalKg());
                }
            }
        }
        assertThat(s.collectedKg()).isEqualByComparingTo(expected);
        assertThat(s.collectedKg().signum()).isPositive();
    }
}
