package com.ase.wastems.service;

import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStopStatus;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operational dashboard summary (US-35). Resource counts are current and period independent;
 * tour counts cover tours planned in the period; stop progress and collected kilograms cover
 * PLANNED, IN_PROGRESS and COMPLETED tours (drafts are not committed work, a cancelled tour's
 * pending stops will never be collected).
 */
@Service
public class DashboardService {

    private static final List<TourStatus> PROGRESS = List.of(TourStatus.PLANNED, TourStatus.IN_PROGRESS,
            TourStatus.COMPLETED);

    private final VehicleRepository vehicles;
    private final AgentRepository agents;
    private final CollectionPointRepository points;
    private final TourRepository tours;

    public DashboardService(VehicleRepository vehicles, AgentRepository agents, CollectionPointRepository points,
            TourRepository tours) {
        this.vehicles = vehicles;
        this.agents = agents;
        this.points = points;
        this.tours = tours;
    }

    @Transactional(readOnly = true)
    public OperationsSummary operations(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("to", "must not be before from");
        }
        long vAvail = vehicles.countByStatus(ResourceStatus.AVAILABLE);
        long vUnavail = vehicles.countByStatus(ResourceStatus.UNAVAILABLE);
        long aAvail = agents.countByStatus(ResourceStatus.AVAILABLE);
        long aUnavail = agents.countByStatus(ResourceStatus.UNAVAILABLE);
        long pActive = points.countByStatus(CollectionPointStatus.ACTIVE);
        long pInactive = points.countByStatus(CollectionPointStatus.INACTIVE);

        Map<TourStatus, Long> t = new EnumMap<>(TourStatus.class);
        for (Object[] row : tours.countToursByStatus(from, to)) {
            t.put((TourStatus) row[0], (Long) row[1]);
        }
        Map<TourStopStatus, Long> s = new EnumMap<>(TourStopStatus.class);
        for (Object[] row : tours.countStopsByStatus(PROGRESS, from, to)) {
            s.put((TourStopStatus) row[0], (Long) row[1]);
        }
        BigDecimal kg = tours.sumCollectedKg(PROGRESS, from, to);
        long pending = s.getOrDefault(TourStopStatus.PENDING, 0L);
        long collected = s.getOrDefault(TourStopStatus.COLLECTED, 0L);
        long notCollected = s.getOrDefault(TourStopStatus.NOT_COLLECTED, 0L);
        long tourTotal = t.values().stream().mapToLong(Long::longValue).sum();
        return new OperationsSummary(from, to, Instant.now().truncatedTo(ChronoUnit.MILLIS),
                new OperationsSummary.ResourceCounts(vAvail + vUnavail, vAvail, vUnavail),
                new OperationsSummary.ResourceCounts(aAvail + aUnavail, aAvail, aUnavail),
                new OperationsSummary.PointCounts(pActive + pInactive, pActive, pInactive),
                new OperationsSummary.TourCounts(tourTotal, t.getOrDefault(TourStatus.DRAFT, 0L),
                        t.getOrDefault(TourStatus.PLANNED, 0L), t.getOrDefault(TourStatus.IN_PROGRESS, 0L),
                        t.getOrDefault(TourStatus.COMPLETED, 0L), t.getOrDefault(TourStatus.CANCELLED, 0L)),
                new OperationsSummary.StopCounts(pending + collected + notCollected, pending, collected,
                        notCollected),
                (kg == null ? BigDecimal.ZERO : kg).setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
