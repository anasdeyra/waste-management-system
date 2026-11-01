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
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.domain.ProposalDecision;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.TourStopStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.AgentRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** The reproducible MN01 demonstration on the real snapshots (S7, US-33). */
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

    private static String bd(BigDecimal v) {
        return v.setScale(2).toPlainString();
    }

    @Test
    void firstLoadCreatesEveryLifecycleState() {
        DemoLoadReport r = demo.load(DemoTrigger.MANUAL);
        assertThat(r.categories()).isEqualTo(new DemoCount(2, 1));
        assertThat(r.demandEntries()).isEqualTo(new DemoCount(4, 0));
        assertThat(r.vehicles()).isEqualTo(new DemoCount(3, 0));
        assertThat(r.accounts()).isEqualTo(new DemoCount(2, 1));
        assertThat(r.agents()).isEqualTo(new DemoCount(5, 0));
        assertThat(r.tours()).isEqualTo(new DemoCount(4, 0));
        assertThat(r.warnings()).isEmpty();
        assertThat(r.tourStates().stream().map(s -> s.status()).toList()).containsExactly(
                TourStatus.DRAFT, TourStatus.PLANNED, TourStatus.IN_PROGRESS, TourStatus.COMPLETED);

        assertThat(vehicles.findAll()).allMatch(v -> v.getOrigin() == DataOrigin.SYNTHETIC);
        assertThat(vehicles.findByRegistration("DEMO-03").orElseThrow().getStatus()).isEqualTo(ResourceStatus.UNAVAILABLE);
        List<Agent> all = agents.findAll();
        assertThat(all).hasSize(5).allMatch(a -> a.getOrigin() == DataOrigin.SYNTHETIC);
        Map<String, Agent> byName = all.stream().collect(Collectors.toMap(Agent::getDisplayName, a -> a));
        assertThat(byName.get("Eli Novak (demo)").getStatus()).isEqualTo(ResourceStatus.UNAVAILABLE);
        assertThat(byName.get("Alex Rivera (demo)").getUserAccount().getUsername()).isEqualTo("agent");
        assertThat(byName.get("Blair Chen (demo)").getUserAccount().getUsername()).isEqualTo("collector");
        assertThat(byName.get("Casey Morgan (demo)").getUserAccount().getUsername()).isEqualTo("agent2");
        assertThat(byName.get("Dana Lopez (demo)").getUserAccount()).isNull();
        assertThat(byName.get("Eli Novak (demo)").getUserAccount()).isNull();

        Tour draft = tourOf(r, "tour:draft");
        assertThat(draft.getOrigin()).isEqualTo(DataOrigin.SYNTHETIC);
        assertThat(draft.getStops()).hasSize(8);
        assertThat(draft.hasRoute()).isFalse();
        List<EvaluationRun> draftRuns = evaluationService.listForTour(draft.getId());
        assertThat(draftRuns).hasSize(1);
        EvaluationRun run = draftRuns.get(0);
        assertThat(run.getDecision()).isNull();
        Map<PlanningMethod, Long> meters = run.getResults().stream().collect(
                Collectors.toMap(MethodResult::getMethod, m -> Math.round(m.getDistanceM())));
        assertThat(meters).containsEntry(PlanningMethod.INPUT_ORDER, 11736L)
                .containsEntry(PlanningMethod.NEAREST_NEIGHBOR, 10212L)
                .containsEntry(PlanningMethod.NN_PLUS_2_OPT, 7470L);

        Tour planned = tourOf(r, "tour:planned");
        assertThat(planned.getRouteMethod()).isEqualTo(PlanningMethod.NN_PLUS_2_OPT);
        assertThat(Math.round(planned.getRouteDistanceM())).isEqualTo(2386L);
        assertThat(planned.getStops().stream().map(s -> s.getCollectionPoint().getSourceIdentifier()).toList())
                .containsExactly("10130144", "10130005", "10130062", "10130055", "10130038");
        assertThat(planned.getStops()).allMatch(s -> s.getStatus() == TourStopStatus.PENDING);
        EvaluationRun accepted = evaluationRuns.findById(planned.getRouteEvaluationRunId()).orElseThrow();
        assertThat(accepted.getDecision()).isEqualTo(ProposalDecision.ACCEPTED);
        assertThat(accepted.getDecidedBy()).isEqualTo("demo-loader");
        TourStop multi = stopsBySource(planned).get("10130062");
        assertThat(multi.getStopDemands().stream().collect(Collectors.toMap(
                d -> d.getCategory().getName(), d -> bd(d.getPlannedQuantityKg()))))
                .containsEntry("Mixed litter", "17.00")
                .containsEntry("Recyclables", "6.00")
                .containsEntry("Organics", "4.00");

        Tour running = tourOf(r, "tour:in-progress");
        assertThat(running.getStartedBy()).isEqualTo("agent");
        Map<String, TourStop> rs = stopsBySource(running);
        TourStop first = rs.get("10130118");
        assertThat(first.getVisitOrder()).isEqualTo(1);
        assertThat(first.getStatus()).isEqualTo(TourStopStatus.COLLECTED);
        assertThat(first.getActualQuantities().stream().collect(Collectors.toMap(
                q -> q.getCategory().getName(), q -> bd(q.getActualQuantityKg()))))
                .containsEntry("Mixed litter", "10.00").containsEntry("Organics", "5.00");
        assertThat(first.getOutcomeRecordedBy().getUsername()).isEqualTo("agent");
        TourStop second = rs.get("10130136");
        assertThat(second.getVisitOrder()).isEqualTo(2);
        assertThat(second.getStatus()).isEqualTo(TourStopStatus.NOT_COLLECTED);
        assertThat(second.getIncidentReason()).isEqualTo(
                "Basket blocked by a parked delivery truck (synthetic demo outcome).");
        assertThat(running.getStops().stream().filter(s -> s.getVisitOrder() > 2))
                .hasSize(3).allMatch(s -> s.getStatus() == TourStopStatus.PENDING);

        Tour done = tourOf(r, "tour:completed");
        assertThat(done.getCompletedBy()).isEqualTo("agent2");
        Map<String, TourStop> ds = stopsBySource(done);
        assertThat(ds.get("10110371").getStatus()).isEqualTo(TourStopStatus.COLLECTED);
        assertThat(bd(ds.get("10110371").actualTotalKg())).isEqualTo("24.00");
        assertThat(bd(ds.get("10110195").actualTotalKg())).isEqualTo("14.40");
        assertThat(bd(ds.get("10110220").actualTotalKg())).isEqualTo("16.00");
        assertThat(ds.get("10110072").getStatus()).isEqualTo(TourStopStatus.NOT_COLLECTED);
    }

    @Test
    void secondLoadIsIdempotent() {
        DemoLoadReport first = demo.load(DemoTrigger.MANUAL);
        long runsBefore = evaluationRuns.count();
        DemoLoadReport second = demo.load(DemoTrigger.MANUAL);
        assertThat(second.categories()).isEqualTo(new DemoCount(0, 3));
        assertThat(second.demandEntries()).isEqualTo(new DemoCount(0, 4));
        assertThat(second.vehicles()).isEqualTo(new DemoCount(0, 3));
        assertThat(second.accounts()).isEqualTo(new DemoCount(0, 3));
        assertThat(second.agents()).isEqualTo(new DemoCount(0, 5));
        assertThat(second.tours()).isEqualTo(new DemoCount(0, 4));
        assertThat(second.warnings()).isEmpty();
        assertThat(second.tourStates()).isEqualTo(first.tourStates().stream().map(s -> s).toList());
        assertThat(demoRecords.count()).isEqualTo(22);
        assertThat(tours.count()).isEqualTo(4);
        assertThat(vehicles.count()).isEqualTo(3);
        assertThat(users.count()).isEqualTo(3);
        assertThat(agents.count()).isEqualTo(5);
        assertThat(categories.count()).isEqualTo(3);
        assertThat(evaluationRuns.count()).isEqualTo(runsBefore);
        assertThat(stopsBySource(tourOf(first, "tour:planned")).get("10130062").getStopDemands()).hasSize(3);
    }

    @Test
    void loadDoesNotResetReviewerProgress() {
        DemoLoadReport r = demo.load(DemoTrigger.MANUAL);
        Tour planned = tourOf(r, "tour:planned");
        executionService.start(planned.getId(), "collector");
        TourStop stop = tourService.get(planned.getId()).getStops().get(0);
        executionService.recordOutcome(planned.getId(), stop.getId(), "collector",
                new StopOutcomeCommand(TourStopStatus.NOT_COLLECTED, List.of(), "Reviewer test"));
        Vehicle v = vehicles.findByRegistration("DEMO-01").orElseThrow();
        v.setStatus(ResourceStatus.UNAVAILABLE);
        vehicles.save(v);
        Agent blair = agents.findAll().stream().filter(a -> a.getDisplayName().startsWith("Blair")).findFirst().orElseThrow();
        blair.unlinkUserAccount();
        agents.save(blair);

        DemoLoadReport again = demo.load(DemoTrigger.MANUAL);
        Tour after = tourService.get(planned.getId());
        assertThat(after.getStatus()).isEqualTo(TourStatus.IN_PROGRESS);
        assertThat(after.getStops().get(0).getStatus()).isEqualTo(TourStopStatus.NOT_COLLECTED);
        assertThat(vehicles.findByRegistration("DEMO-01").orElseThrow().getStatus()).isEqualTo(ResourceStatus.UNAVAILABLE);
        assertThat(agents.findById(blair.getId()).orElseThrow().getUserAccount()).isNull();
        assertThat(again.tours()).isEqualTo(new DemoCount(0, 4));
        assertThat(tours.count()).isEqualTo(4);
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
        assertThat(r.vehicles()).isEqualTo(new DemoCount(2, 1));
        assertThat(r.accounts()).isEqualTo(new DemoCount(1, 2));
        assertThat(r.tours()).isEqualTo(new DemoCount(4, 0));
        String warnings = String.join("\n", r.warnings());
        assertThat(warnings).contains("DEMO-01", "'agent'", "'agent2'", "10130005", "Recyclables");
        assertThat(r.warnings().stream().filter(w -> w.contains("no crew member with a linked active AGENT account")))
                .hasSize(2);
        assertThat(r.tourStates().stream().map(s -> s.status()).toList()).containsExactly(
                TourStatus.DRAFT, TourStatus.PLANNED, TourStatus.PLANNED, TourStatus.PLANNED);
        assertThat(tourOf(r, "tour:planned").getStops()).hasSize(4);
        assertThat(agents.findById(ann.getId()).orElseThrow().getUserAccount().getUsername()).isEqualTo("agent");

        DemoLoadReport second = demo.load(DemoTrigger.MANUAL);
        assertThat(second.tours().created()).isZero();
        assertThat(tours.count()).isEqualTo(4);
    }

    @Test
    void missingImportIsRefused() {
        points.deleteAll();
        depots.deleteAll();
        assertThatThrownBy(() -> demo.load(DemoTrigger.MANUAL)).isInstanceOf(BusinessRuleException.class);
        assertThat(demoRecords.count()).isZero();
    }

    @Test
    void demoEndpointRoleMatrix() throws Exception {
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        mvc.perform(post("/api/admin/demo").with(user("agent1").roles("AGENT")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/demo").with(user("planner1").roles("PLANNER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/demo").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/demo").with(user("admin1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tours.created").value(4));
        mvc.perform(get("/api/admin/demo/report").with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("MANUAL"));
    }
}
