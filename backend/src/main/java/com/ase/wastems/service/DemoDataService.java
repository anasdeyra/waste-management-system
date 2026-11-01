package com.ase.wastems.service;

import com.ase.wastems.config.DemoProperties;
import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.DemoEntityType;
import com.ase.wastems.domain.DemoRecord;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.StopDemand;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.TourStopStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DemoRecordRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reproducible, idempotent MN01 demonstration (US-33, D-048): synthetic categories, demand
 * entries, vehicles, agents, accounts and four tours (draft, planned, in progress, completed)
 * on the real MN01 baskets and garage. Every record is identified by a {@link DemoRecord}; a key
 * with a record is never created again, existing data is adopted and never modified, and each
 * tour only advances forward toward its target state. The class is deliberately not
 * transactional: item creation uses explicit transaction blocks, the lifecycle steps run in the
 * transactions of the services they call, so a refused step never poisons another.
 */
@Service
public class DemoDataService {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataService.class);

    static final String BASKET_DATASET = "8znf-7b2c";
    static final String DEPOT_DATASET = "xw3j-2yxf";
    static final String DEPOT_ID = "MN01G";
    static final String ACTOR = "demo-loader";
    private static final String SYNTHETIC_MARK = " (synthetic demo category)";

    private record CategorySpec(String name, String description) {}

    private record ExtraDemand(String basketId, String category, String kg) {}

    private record VehicleSpec(String registration, String capacityKg, ResourceStatus status) {}

    private record AccountSpec(String username, String password) {}

    private record AgentSpec(String key, String displayName, ResourceStatus status, String account) {}

    private record TourSpec(
            String key,
            TourStatus target,
            String vehicle,
            String driver,
            List<String> collectors,
            List<String> baskets) {}

    private static final List<CategorySpec> CATEGORIES = List.of(
            new CategorySpec("Mixed litter", "Street litter-basket refuse" + SYNTHETIC_MARK),
            new CategorySpec("Recyclables", "Metal, glass, plastic and paper" + SYNTHETIC_MARK),
            new CategorySpec("Organics", "Food and yard waste" + SYNTHETIC_MARK));
    private static final List<ExtraDemand> EXTRA_DEMANDS = List.of(
            new ExtraDemand("10130038", "Recyclables", "9"),
            new ExtraDemand("10130062", "Recyclables", "6"),
            new ExtraDemand("10130062", "Organics", "4"),
            new ExtraDemand("10130118", "Organics", "5"));
    private static final List<VehicleSpec> VEHICLES = List.of(
            new VehicleSpec("DEMO-01", "2000.00", ResourceStatus.AVAILABLE),
            new VehicleSpec("DEMO-02", "1500.00", ResourceStatus.AVAILABLE),
            new VehicleSpec("DEMO-03", "1800.00", ResourceStatus.UNAVAILABLE));
    private static final List<AccountSpec> ACCOUNTS = List.of(
            new AccountSpec("agent", "agent123"),
            new AccountSpec("collector", "collector123"),
            new AccountSpec("agent2", "agent2123"));
    private static final List<AgentSpec> AGENTS = List.of(
            new AgentSpec("alex", "Alex Rivera (demo)", ResourceStatus.AVAILABLE, "agent"),
            new AgentSpec("blair", "Blair Chen (demo)", ResourceStatus.AVAILABLE, "collector"),
            new AgentSpec("casey", "Casey Morgan (demo)", ResourceStatus.AVAILABLE, "agent2"),
            new AgentSpec("dana", "Dana Lopez (demo)", ResourceStatus.AVAILABLE, null),
            new AgentSpec("eli", "Eli Novak (demo)", ResourceStatus.UNAVAILABLE, null));
    private static final List<TourSpec> TOURS = List.of(
            new TourSpec("tour:draft", TourStatus.DRAFT, "DEMO-01", "alex", List.of("blair"),
                    List.of("10130311", "10130009", "10110472", "10130156", "10130147", "10110073", "10130335",
                            "10130346")),
            new TourSpec("tour:planned", TourStatus.PLANNED, "DEMO-01", "alex", List.of("blair"),
                    List.of("10130038", "10130055", "10130062", "10130005", "10130144")),
            new TourSpec("tour:in-progress", TourStatus.IN_PROGRESS, "DEMO-02", "alex", List.of("casey", "dana"),
                    List.of("10130057", "10130136", "10130179", "10130118", "10130044")),
            new TourSpec("tour:completed", TourStatus.COMPLETED, "DEMO-02", "casey", List.of("alex"),
                    List.of("10110072", "10110195", "10110371", "10110220")));
    private static final BigDecimal[] FACTORS = {
        new BigDecimal("1.00"), new BigDecimal("0.90"), new BigDecimal("0.80")
    };
    private static final String BLOCKED = "Basket blocked by a parked delivery truck (synthetic demo outcome).";
    private static final String STREET_WORKS = "Street works prevented access to the basket (synthetic demo outcome).";

    /** Mutable created/existing counter of one record type. */
    private static final class Counter {
        int created;
        int existing;

        DemoCount toCount() {
            return new DemoCount(created, existing);
        }
    }

    private final DemoProperties props;
    private final DemoRecordRepository records;
    private final WasteCategoryRepository categories;
    private final CollectionPointRepository points;
    private final DepotRepository depots;
    private final VehicleRepository vehicles;
    private final AppUserRepository users;
    private final AgentRepository agents;
    private final TourRepository tours;
    private final EvaluationRunRepository runs;
    private final TourService tourService;
    private final EvaluationService evaluationService;
    private final TourExecutionService executionService;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private volatile DemoLoadReport last;

    public DemoDataService(
            DemoProperties props,
            DemoRecordRepository records,
            WasteCategoryRepository categories,
            CollectionPointRepository points,
            DepotRepository depots,
            VehicleRepository vehicles,
            AppUserRepository users,
            AgentRepository agents,
            TourRepository tours,
            EvaluationRunRepository runs,
            TourService tourService,
            EvaluationService evaluationService,
            TourExecutionService executionService,
            PasswordEncoder encoder,
            AuditService audit,
            PlatformTransactionManager txManager) {
        this.audit = audit;
        this.props = props;
        this.records = records;
        this.categories = categories;
        this.points = points;
        this.depots = depots;
        this.vehicles = vehicles;
        this.users = users;
        this.agents = agents;
        this.tours = tours;
        this.runs = runs;
        this.tourService = tourService;
        this.evaluationService = evaluationService;
        this.executionService = executionService;
        this.encoder = encoder;
        this.tx = new TransactionTemplate(txManager);
    }

    public Optional<DemoLoadReport> lastReport() {
        return Optional.ofNullable(last);
    }

    public boolean isEnabled() {
        return props.enabled();
    }

    /** Loads (or completes) the demonstration; 409 when disabled, 422 without the MN01 import. */
    public synchronized DemoLoadReport load(DemoTrigger trigger) {
        if (!props.enabled()) {
            throw new ConflictException("Demo loading is disabled (wastems.demo.enabled=false).");
        }
        Depot depot = depots.findBySourceDatasetAndSourceIdentifier(DEPOT_DATASET, DEPOT_ID).orElse(null);
        if (depot == null || !points.existsBySourceDataset(BASKET_DATASET)) {
            throw new BusinessRuleException(
                    "The MN01 import has not run yet; import the MN01 snapshots first (POST /api/admin/import).");
        }
        List<String> warnings = new ArrayList<>();
        Counter cats = new Counter();
        Counter demands = new Counter();
        Counter vehs = new Counter();
        Counter accts = new Counter();
        Counter ags = new Counter();
        Counter trs = new Counter();

        for (CategorySpec c : CATEGORIES) {
            tx.executeWithoutResult(s -> loadCategory(c, cats));
        }
        for (CategorySpec c : CATEGORIES) {
            categories.findByNameIgnoreCase(c.name()).filter(x -> !x.isActive()).ifPresent(x -> warnings.add(
                    "Category " + c.name() + " is inactive; demo entries that need it are skipped."));
        }
        for (ExtraDemand d : EXTRA_DEMANDS) {
            tx.executeWithoutResult(s -> loadDemand(d, demands, warnings));
        }
        for (VehicleSpec v : VEHICLES) {
            tx.executeWithoutResult(s -> loadVehicle(v, vehs, warnings));
        }
        for (AccountSpec a : ACCOUNTS) {
            tx.executeWithoutResult(s -> loadAccount(a, accts, warnings));
        }
        for (AgentSpec a : AGENTS) {
            tx.executeWithoutResult(s -> loadAgent(a, ags, warnings));
        }
        for (TourSpec t : TOURS) {
            createTour(t, depot, trs, warnings);
        }
        for (TourSpec t : TOURS) {
            advance(t, depot, warnings);
        }
        List<DemoTourState> states = new ArrayList<>();
        for (TourSpec t : TOURS) {
            records.findByDemoKey(t.key()).flatMap(r -> tours.findById(r.getEntityId())).ifPresent(
                    tour -> states.add(new DemoTourState(t.key(), tour.getId(), t.target(), tour.getStatus())));
        }
        DemoLoadReport report = new DemoLoadReport(trigger, Instant.now().truncatedTo(ChronoUnit.MILLIS),
                cats.toCount(), demands.toCount(), vehs.toCount(), accts.toCount(), ags.toCount(), trs.toCount(),
                List.copyOf(states), List.copyOf(warnings));
        last = report;
        audit.record(
                com.ase.wastems.domain.AuditAction.DEMO_LOADED,
                com.ase.wastems.domain.AuditEntityType.DEMO_DATA,
                null,
                trigger
                        + ": created categories "
                        + cats.created
                        + ", demand entries "
                        + demands.created
                        + ", vehicles "
                        + vehs.created
                        + ", accounts "
                        + accts.created
                        + ", agents "
                        + ags.created
                        + ", tours "
                        + trs.created
                        + "; warnings "
                        + warnings.size());
        LOG.info("Demo load ({}): categories created={}, existing={}; demand entries created={}, existing={};"
                        + " vehicles created={}, existing={}; accounts created={}, existing={}; agents created={},"
                        + " existing={}; tours created={}, existing={}; warnings={}",
                trigger, cats.created, cats.existing, demands.created, demands.existing, vehs.created,
                vehs.existing, accts.created, accts.existing, ags.created, ags.existing, trs.created,
                trs.existing, warnings.size());
        warnings.forEach(w -> LOG.warn("Demo load warning: {}", w));
        return report;
    }

    private void record(String key, DemoEntityType type, UUID entityId) {
        records.save(new DemoRecord(key, type, entityId, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    }

    private void loadCategory(CategorySpec spec, Counter n) {
        String key = "category:" + spec.name();
        if (records.findByDemoKey(key).isPresent()) {
            n.existing++;
            return;
        }
        Optional<WasteCategory> found = categories.findByNameIgnoreCase(spec.name());
        WasteCategory c;
        if (found.isPresent()) {
            c = found.get();
            n.existing++;
        } else {
            c = categories.save(new WasteCategory(spec.name(), spec.description()));
            n.created++;
        }
        record(key, DemoEntityType.CATEGORY, c.getId());
    }

    private void loadDemand(ExtraDemand spec, Counter n, List<String> warnings) {
        String key = "demand:" + spec.basketId() + ":" + spec.category();
        if (records.findByDemoKey(key).isPresent()) {
            n.existing++;
            return;
        }
        CollectionPoint point = points
                .findWithDemandsBySourceDatasetAndSourceIdentifier(BASKET_DATASET, spec.basketId())
                .orElse(null);
        if (point == null) {
            warnings.add("Basket " + spec.basketId() + " is missing; demand " + spec.category() + " skipped.");
            return;
        }
        WasteCategory category = categories.findByNameIgnoreCase(spec.category()).orElse(null);
        if (category == null || !category.isActive()) {
            warnings.add("Basket " + spec.basketId() + ": category " + spec.category() + " is inactive; demand skipped.");
            return;
        }
        Optional<DemandEntry> existing = point.getDemands().stream()
                .filter(d -> d.getCategory().getId().equals(category.getId()))
                .findFirst();
        if (existing.isPresent()) {
            n.existing++;
            record(key, DemoEntityType.DEMAND_ENTRY, existing.get().getId());
            return;
        }
        DemandEntry entry = new DemandEntry(category, new BigDecimal(spec.kg()), DataOrigin.SYNTHETIC);
        point.addDemand(entry);
        points.flush();
        n.created++;
        record(key, DemoEntityType.DEMAND_ENTRY, entry.getId());
    }

    private void loadVehicle(VehicleSpec spec, Counter n, List<String> warnings) {
        String key = "vehicle:" + spec.registration();
        if (records.findByDemoKey(key).isPresent()) {
            n.existing++;
            return;
        }
        Optional<Vehicle> found = vehicles.findByRegistration(spec.registration());
        Vehicle v;
        if (found.isPresent()) {
            v = found.get();
            n.existing++;
            if (v.getOrigin() != DataOrigin.SYNTHETIC) {
                warnings.add("Vehicle " + spec.registration() + " already exists and is not synthetic; it is reused as is.");
            }
        } else {
            v = new Vehicle(spec.registration(), new BigDecimal(spec.capacityKg()), DataOrigin.SYNTHETIC);
            v.setStatus(spec.status());
            v = vehicles.save(v);
            n.created++;
        }
        record(key, DemoEntityType.VEHICLE, v.getId());
    }

    private void loadAccount(AccountSpec spec, Counter n, List<String> warnings) {
        String key = "account:" + spec.username();
        if (records.findByDemoKey(key).isPresent()) {
            n.existing++;
            return;
        }
        Optional<AppUser> found = users.findByUsername(spec.username());
        AppUser u;
        if (found.isPresent()) {
            u = found.get();
            n.existing++;
            if (u.getRole() != AppRole.AGENT) {
                warnings.add("Account '" + spec.username() + "' exists with role " + u.getRole()
                        + "; it is not linked to a demo agent.");
            }
        } else {
            u = users.save(new AppUser(spec.username(), encoder.encode(spec.password()), AppRole.AGENT));
            n.created++;
        }
        record(key, DemoEntityType.ACCOUNT, u.getId());
    }

    private void loadAgent(AgentSpec spec, Counter n, List<String> warnings) {
        String key = "agent:" + spec.key();
        if (records.findByDemoKey(key).isPresent()) {
            n.existing++;
            return;
        }
        Agent agent = new Agent(spec.displayName(), DataOrigin.SYNTHETIC);
        agent.setStatus(spec.status());
        if (spec.account() != null) {
            AppUser account = users.findByUsername(spec.account()).orElse(null);
            if (account != null && account.getRole() == AppRole.AGENT) {
                Optional<Agent> owner = agents.findByUserAccount(account);
                if (owner.isPresent()) {
                    warnings.add("Account '" + spec.account() + "' is already linked to agent '"
                            + owner.get().getDisplayName() + "'; demo agent '" + spec.displayName()
                            + "' has no login.");
                } else {
                    agent.linkUserAccount(account);
                }
            }
        }
        agent = agents.save(agent);
        n.created++;
        record(key, DemoEntityType.AGENT, agent.getId());
    }

    private void createTour(TourSpec spec, Depot depot, Counter n, List<String> warnings) {
        if (records.findByDemoKey(spec.key()).isPresent()) {
            n.existing++;
            return;
        }
        try {
            boolean created = Boolean.TRUE.equals(tx.execute(s -> doCreateTour(spec, depot, warnings)));
            if (created) {
                n.created++;
            }
        } catch (RuntimeException e) {
            warnings.add("Tour " + spec.key() + ": creation failed: " + e.getMessage());
        }
    }

    /** Runs inside one transaction block; pre-checks everything so that no service call throws. */
    private boolean doCreateTour(TourSpec spec, Depot depot, List<String> warnings) {
        Vehicle vehicle = recordedEntity("vehicle:" + spec.vehicle()).flatMap(vehicles::findById).orElse(null);
        Agent driver = recordedEntity("agent:" + spec.driver()).flatMap(agents::findById).orElse(null);
        List<Agent> collectors = new ArrayList<>();
        boolean missing = vehicle == null || driver == null;
        for (String c : spec.collectors()) {
            Agent a = recordedEntity("agent:" + c).flatMap(agents::findById).orElse(null);
            if (a == null) {
                missing = true;
            } else {
                collectors.add(a);
            }
        }
        if (missing) {
            warnings.add("Tour " + spec.key() + ": demo vehicle or agent is missing; tour skipped.");
            return false;
        }
        if (!vehicle.isAvailable()) {
            warnings.add("Tour " + spec.key() + ": " + vehicle.getRegistration() + " is unavailable; tour skipped.");
            return false;
        }
        List<Agent> crew = new ArrayList<>(collectors);
        crew.add(0, driver);
        for (Agent a : crew) {
            if (!a.isAvailable()) {
                warnings.add("Tour " + spec.key() + ": " + a.getDisplayName() + " is unavailable; tour skipped.");
                return false;
            }
        }
        List<CollectionPoint> kept = new ArrayList<>();
        for (String id : spec.baskets()) {
            Optional<CollectionPoint> p =
                    points.findWithDemandsBySourceDatasetAndSourceIdentifier(BASKET_DATASET, id);
            if (p.isPresent() && p.get().isOptimizationEligible()) {
                kept.add(p.get());
            } else {
                warnings.add("Tour " + spec.key() + ": basket " + id + " is missing, inactive or not eligible; skipped.");
            }
        }
        if (kept.size() < 2) {
            warnings.add("Tour " + spec.key() + ": fewer than two eligible baskets; tour skipped.");
            return false;
        }
        UUID tourId = tourService.create(LocalDate.now(), depot.getId(), DataOrigin.SYNTHETIC).getId();
        for (CollectionPoint p : kept) {
            tourService.addStop(tourId, p.getId());
        }
        tourService.assign(tourId, vehicle.getId(), driver.getId(), collectors.stream().map(Agent::getId).toList());
        record(spec.key(), DemoEntityType.TOUR, tourId);
        return true;
    }

    private Optional<UUID> recordedEntity(String key) {
        return records.findByDemoKey(key).map(DemoRecord::getEntityId);
    }

    private static int rank(TourStatus s) {
        return switch (s) {
            case DRAFT -> 0;
            case PLANNED -> 1;
            case IN_PROGRESS -> 2;
            case COMPLETED -> 3;
            case CANCELLED -> -1;
        };
    }

    /** Moves the tour forward, one guarded step at a time; a failing step stops this tour only. */
    private void advance(TourSpec spec, Depot depot, List<String> warnings) {
        Optional<UUID> recorded = recordedEntity(spec.key());
        if (recorded.isEmpty()) {
            return;
        }
        UUID id = recorded.get();
        String step = "load";
        try {
            Tour t = tourService.get(id);
            int target = rank(spec.target());
            int current = rank(t.getStatus());
            boolean guardedAtTarget = spec.target() == TourStatus.DRAFT || spec.target() == TourStatus.IN_PROGRESS;
            if (current < 0 || current > target || (current == target && !guardedAtTarget)) {
                return;
            }
            if (t.isDraft() && runs.countByTourId(id) == 0) {
                step = "evaluation";
                evaluationService.evaluate(tourService.loadDraftAndBuildScenario(id), ACTOR);
                t = tourService.get(id);
            }
            if (target >= 1 && t.isDraft() && !t.hasRoute()) {
                step = "proposal acceptance";
                EvaluationRun run = runs.findByTourIdOrderByCreatedAtDesc(id).stream().findFirst().orElse(null);
                if (run == null || run.getStatus() != EvaluationStatus.COMPLETED
                        || run.getTourRevision() == null || run.getTourRevision() != t.getRevision()) {
                    run = evaluationService.evaluate(tourService.loadDraftAndBuildScenario(id), ACTOR);
                }
                tourService.acceptOptimizedProposal(id, run.getId(), ACTOR);
                t = tourService.get(id);
            }
            if (target >= 1 && t.isDraft() && t.hasRoute()) {
                step = "confirmation";
                if (t.getPlanningDate().isBefore(LocalDate.now())) {
                    tourService.updateHeader(id, LocalDate.now(), depot.getId());
                }
                tourService.confirm(id);
                t = tourService.get(id);
            }
            if (target >= 2 && t.getStatus() == TourStatus.PLANNED) {
                step = "start";
                Optional<String> actor = actorOf(t);
                if (actor.isEmpty()) {
                    warnings.add("Tour " + spec.key()
                            + ": no crew member with a linked active AGENT account; it stays PLANNED.");
                    return;
                }
                executionService.start(id, actor.get());
                t = tourService.get(id);
            }
            if (target >= 2 && t.getStatus() == TourStatus.IN_PROGRESS) {
                step = target == 3 ? "completion" : "stop outcomes";
                Optional<String> actor = actorOf(t);
                if (actor.isEmpty()) {
                    warnings.add("Tour " + spec.key()
                            + ": no crew member with a linked active AGENT account; outcomes are not recorded.");
                    return;
                }
                recordOutcomes(id, t, actor.get(), target == 3 ? t.getStops().size() : 2, target == 3);
                if (target == 3) {
                    executionService.complete(id, actor.get());
                }
            }
        } catch (RuntimeException e) {
            warnings.add("Tour " + spec.key() + ": " + step + " not possible: " + e.getMessage());
        }
    }

    /** First crew member, driver first, with a linked active AGENT account. */
    private Optional<String> actorOf(Tour t) {
        List<Agent> crew = new ArrayList<>();
        if (t.getDriver() != null) {
            crew.add(t.getDriver());
        }
        crew.addAll(t.getCollectors());
        for (Agent a : crew) {
            AppUser u = a.getUserAccount();
            if (u != null && u.getRole() == AppRole.AGENT && u.isActive()) {
                return Optional.of(u.getUsername());
            }
        }
        return Optional.empty();
    }

    /**
     * Scripted outcomes of the still-pending stops up to {@code upTo} visit orders; the last stop
     * of a tour that is completed becomes NOT_COLLECTED.
     */
    private void recordOutcomes(UUID tourId, Tour t, String actor, int upTo, boolean toCompletion) {
        int n = t.getStops().size();
        for (TourStop s : t.getStops()) {
            if (s.getVisitOrder() > upTo || s.getStatus() != TourStopStatus.PENDING) {
                continue;
            }
            StopOutcomeCommand cmd;
            if (toCompletion ? s.getVisitOrder() == n : s.getVisitOrder() == 2) {
                cmd = new StopOutcomeCommand(TourStopStatus.NOT_COLLECTED, List.of(),
                        toCompletion ? STREET_WORKS : BLOCKED);
            } else {
                BigDecimal factor = FACTORS[(s.getVisitOrder() - 1) % FACTORS.length];
                List<StopOutcomeCommand.QuantityInput> q = new ArrayList<>();
                for (StopDemand d : s.getStopDemands()) {
                    q.add(new StopOutcomeCommand.QuantityInput(d.getCategory().getId(),
                            d.getPlannedQuantityKg().multiply(factor).setScale(2, RoundingMode.HALF_UP)));
                }
                cmd = new StopOutcomeCommand(TourStopStatus.COLLECTED, q, null);
            }
            executionService.recordOutcome(tourId, s.getId(), actor, cmd);
        }
    }
}
