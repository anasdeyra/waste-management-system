package com.ase.wastems.service;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
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

    public TourService(
            TourRepository tours,
            DepotRepository depots,
            CollectionPointRepository points,
            VehicleRepository vehicles,
            AgentRepository agents) {
        this.tours = tours;
        this.depots = depots;
        this.points = points;
        this.vehicles = vehicles;
        this.agents = agents;
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
