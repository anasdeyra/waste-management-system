package com.ase.wastems.service;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.SourceSnapshot;
import com.ase.wastems.domain.TourIssue;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules of tour drafts (S4): creation, header edits, stop selection and order, and
 * (T3) vehicle/crew assignment. Every returned tour has its lazy collections loaded so that
 * the web layer can map it after the transaction.
 */
@Service
public class TourService {

    private final TourRepository tours;
    private final DepotRepository depots;
    private final CollectionPointRepository points;
    private final VehicleRepository vehicles;
    private final AgentRepository agents;
    private final SourceSnapshotRepository sourceSnapshots;

    public TourService(
            TourRepository tours,
            DepotRepository depots,
            CollectionPointRepository points,
            VehicleRepository vehicles,
            AgentRepository agents,
            SourceSnapshotRepository sourceSnapshots) {
        this.tours = tours;
        this.depots = depots;
        this.points = points;
        this.vehicles = vehicles;
        this.agents = agents;
        this.sourceSnapshots = sourceSnapshots;
    }

    private static Tour initialized(Tour t) {
        t.getCollectors().size();
        for (TourStop s : t.getStops()) {
            s.getStopDemands().size();
        }
        return t;
    }

    @Transactional(readOnly = true)
    public List<Tour> list(TourStatus status, LocalDate planningDate) {
        return tours.search(status, planningDate).stream().map(TourService::initialized).toList();
    }

    @Transactional(readOnly = true)
    public Tour get(UUID id) {
        return initialized(find(id));
    }

    /**
     * Freezes a ready DRAFT into the immutable scenario shared by the three methods (US-19, US-21):
     * the selected points with their names, coordinates and planned demand totals, the depot, the
     * vehicle capacity and the source snapshot identifiers. Read-only: the tour is never touched.
     */
    @Transactional(readOnly = true)
    public FrozenScenario loadDraftAndBuildScenario(UUID tourId) {
        Tour tour = initialized(find(tourId));
        if (!tour.isDraft()) {
            throw new ConflictException(
                    "Only a DRAFT tour can be evaluated; this tour is " + tour.getStatus() + ".");
        }
        List<TourIssue> issues = tour.structuralIssues();
        if (!issues.isEmpty()) {
            throw new BusinessRuleException(
                    "The tour is not ready for evaluation ("
                            + issues.stream().map(i -> i.code().name()).collect(Collectors.joining(", "))
                            + "): "
                            + issues.stream().map(TourIssue::message).collect(Collectors.joining(" ")));
        }
        TreeSet<String> datasets = new TreeSet<>();
        boolean userEntered = false;
        Depot d = tour.getDepot();
        if (d.getSourceDataset() == null) {
            userEntered = true;
        } else {
            datasets.add(d.getSourceDataset());
        }
        List<FrozenStop> stops = new ArrayList<>();
        for (TourStop stop : tour.getStops()) {
            CollectionPoint p = stop.getCollectionPoint();
            if (p.getSourceDataset() == null) {
                userEntered = true;
            } else {
                datasets.add(p.getSourceDataset());
            }
            stops.add(
                    new FrozenStop(
                            stop.getVisitOrder(),
                            p.getId(),
                            p.getSourceDataset(),
                            p.getSourceIdentifier(),
                            p.getName(),
                            p.getLongitude(),
                            p.getLatitude(),
                            stop.plannedTotalKg()));
        }
        List<String> ids = new ArrayList<>();
        for (String ds : datasets) {
            ids.add(
                    sourceSnapshots
                            .findBySourceDataset(ds)
                            .map(SourceSnapshot::getSnapshotId)
                            .orElse(ds + ":unknown"));
        }
        if (userEntered) {
            ids.add("USER_ENTERED");
        }
        BigDecimal capacity = tour.getVehicle().getCapacityKg();
        return new FrozenScenario(
                tour.getId(),
                tour.getRevision(),
                new FrozenDepot(d.getName(), d.getLongitude(), d.getLatitude(), d.getSourceIdentifier()),
                stops,
                capacity,
                ids);
    }

    @Transactional
    public Tour create(LocalDate planningDate, UUID depotId) {
        requireNotPast(planningDate);
        Depot depot = findDepot(depotId);
        return initialized(tours.save(new Tour(planningDate, depot, DataOrigin.USER_ENTERED)));
    }

    @Transactional
    public Tour updateHeader(UUID id, LocalDate planningDate, UUID depotId) {
        Tour tour = find(id);
        requireDraft(tour);
        if (!planningDate.equals(tour.getPlanningDate())) {
            requireNotPast(planningDate);
        }
        Depot depot = findDepot(depotId);
        tour.setPlanningDate(planningDate);
        tour.setDepot(depot);
        tour.touch();
        return initialized(tour);
    }

    @Transactional
    public TourStop addStop(UUID tourId, UUID pointId) {
        Tour tour = find(tourId);
        requireDraft(tour);
        CollectionPoint point =
                points.findWithDemandsById(pointId)
                        .orElseThrow(
                                () -> new NotFoundException("Collection point not found: " + pointId));
        if (!point.isActive()) {
            throw new BusinessRuleException(
                    "Collection point " + point.getName() + " is inactive and cannot be added to a tour.");
        }
        if (!point.isOptimizationEligible()) {
            throw new BusinessRuleException(
                    "Collection point "
                            + point.getName()
                            + " is not eligible for planning (source type "
                            + point.getSourceOwnershipType()
                            + " is not a DSNY service obligation).");
        }
        if (tour.containsPoint(pointId)) {
            throw new ConflictException(
                    "Collection point " + point.getName() + " is already a stop of this tour.");
        }
        TourStop stop = tour.addStop(point);
        tour.touch();
        tours.flush();
        initialized(tour);
        return stop;
    }

    @Transactional
    public Tour removeStop(UUID tourId, UUID stopId) {
        Tour tour = find(tourId);
        requireDraft(tour);
        TourStop stop =
                tour.findStop(stopId)
                        .orElseThrow(() -> new NotFoundException("Tour stop not found: " + stopId));
        tour.removeStop(stop);
        tour.touch();
        return initialized(tour);
    }

    @Transactional
    public Tour reorderStops(UUID tourId, List<UUID> stopIds) {
        Tour tour = find(tourId);
        requireDraft(tour);
        HashSet<UUID> requested = new HashSet<>(stopIds);
        HashSet<UUID> existing = new HashSet<>();
        tour.getStops().forEach(s -> existing.add(s.getId()));
        if (stopIds.size() != tour.getStops().size()
                || requested.size() != stopIds.size()
                || !requested.equals(existing)) {
            throw new BusinessRuleException("The new order must list every stop of the tour exactly once.");
        }
        tour.reorderStops(stopIds);
        tour.touch();
        return initialized(tour);
    }

    /** Replaces vehicle, driver and collectors together; the first failing rule wins. */
    @Transactional
    public Tour assign(UUID tourId, UUID vehicleId, UUID driverId, List<UUID> collectorIds) {
        Tour tour = find(tourId);
        requireDraft(tour);
        List<UUID> collectorIdList = collectorIds == null ? List.of() : collectorIds;
        if (collectorIdList.size() > 2) {
            throw new BusinessRuleException("A tour has at most two collectors.");
        }
        if (new HashSet<>(collectorIdList).size() != collectorIdList.size()) {
            throw new BusinessRuleException("Collectors must be distinct.");
        }
        if (driverId != null && collectorIdList.contains(driverId)) {
            throw new BusinessRuleException("The driver cannot also be a collector.");
        }
        Vehicle vehicle =
                vehicleId == null
                        ? null
                        : vehicles
                                .findById(vehicleId)
                                .orElseThrow(
                                        () -> new NotFoundException("Vehicle not found: " + vehicleId));
        Agent driver = driverId == null ? null : findAgent(driverId);
        List<Agent> collectors = collectorIdList.stream().map(this::findAgent).toList();

        if (vehicle != null
                && (tour.getVehicle() == null || !tour.getVehicle().getId().equals(vehicle.getId()))
                && !vehicle.isAvailable()) {
            throw new BusinessRuleException(
                    "Vehicle " + vehicle.getRegistration() + " is unavailable and cannot be assigned.");
        }
        Set<UUID> onTour = new HashSet<>();
        if (tour.getDriver() != null) {
            onTour.add(tour.getDriver().getId());
        }
        tour.getCollectors().forEach(c -> onTour.add(c.getId()));
        List<Agent> newCrew = new ArrayList<>(collectors);
        if (driver != null) {
            newCrew.add(0, driver);
        }
        for (Agent a : newCrew) {
            if (!onTour.contains(a.getId()) && !a.isAvailable()) {
                throw new BusinessRuleException(
                        "Agent " + a.getDisplayName() + " is unavailable and cannot be assigned.");
            }
        }
        tour.assign(vehicle, driver, collectors);
        tour.touch();
        return initialized(tour);
    }

    private Agent findAgent(UUID id) {
        return agents.findById(id).orElseThrow(() -> new NotFoundException("Agent not found: " + id));
    }

    private Tour find(UUID id) {
        return tours.findById(id).orElseThrow(() -> new NotFoundException("Tour not found: " + id));
    }

    private Depot findDepot(UUID depotId) {
        return depots.findById(depotId)
                .orElseThrow(() -> new NotFoundException("Depot not found: " + depotId));
    }

    private static void requireNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("The planning date must be today or later.");
        }
    }

    private static void requireDraft(Tour t) {
        if (!t.isDraft()) {
            throw new ConflictException(
                    "Only a DRAFT tour can be edited; this tour is " + t.getStatus() + ".");
        }
    }
}
