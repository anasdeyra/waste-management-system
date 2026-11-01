package com.ase.wastems.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plain unit tests of the tour draft domain rules (no Spring). */
class TourTest {

    private final WasteCategory paper = new WasteCategory("Paper", null);
    private final WasteCategory glass = new WasteCategory("Glass", null);
    private final Depot depot =
            new Depot("MN01G", -74.0104, 40.7255, DataOrigin.IMPORTED_REAL, "xw3j-2yxf", "MN01G");

    private Tour newTour() {
        return new Tour(LocalDate.now().plusDays(1), depot, DataOrigin.USER_ENTERED);
    }

    private CollectionPoint point(String name, String... paperThenGlassKg) {
        CollectionPoint p = new CollectionPoint(name, -74.0, 40.71, DataOrigin.USER_ENTERED);
        WasteCategory[] cats = {paper, glass};
        for (int i = 0; i < paperThenGlassKg.length; i++) {
            p.addDemand(
                    new DemandEntry(
                            cats[i], new BigDecimal(paperThenGlassKg[i]), DataOrigin.USER_ENTERED));
        }
        return p;
    }

    private Vehicle vehicle(String capacity) {
        return new Vehicle("NY-1", new BigDecimal(capacity), DataOrigin.USER_ENTERED);
    }

    private Agent agent(String name) {
        return new Agent(name, DataOrigin.USER_ENTERED);
    }

    private List<TourIssueCode> codes(Tour t) {
        return t.structuralIssues().stream().map(TourIssue::code).toList();
    }

    @Test
    void newTourIsEmptyDraft() {
        Tour t = newTour();
        assertEquals(TourStatus.DRAFT, t.getStatus());
        assertEquals(0, t.getRevision());
        assertTrue(t.getStops().isEmpty());
        assertEquals(
                List.of(TourIssueCode.NO_STOPS, TourIssueCode.NO_VEHICLE, TourIssueCode.NO_DRIVER),
                codes(t));
    }

    @Test
    void addStopAppendsAndSnapshotsDemand() {
        Tour t = newTour();
        CollectionPoint a = point("A", "12.50", "7.50");
        TourStop s = t.addStop(a);
        assertEquals(1, s.getVisitOrder());
        assertEquals(2, s.getStopDemands().size());
        assertEquals(0, s.plannedTotalKg().compareTo(new BigDecimal("20.00")));
        assertEquals(TourStopStatus.PENDING, s.getStatus());
        a.getDemands().get(0).setQuantityKg(new BigDecimal("99"));
        assertEquals(0, s.plannedTotalKg().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void removeStopRenumbersContiguously() {
        Tour t = newTour();
        CollectionPoint a = point("A", "1");
        CollectionPoint b = point("B", "1");
        CollectionPoint c = point("C", "1");
        t.addStop(a);
        TourStop sb = t.addStop(b);
        t.addStop(c);
        t.removeStop(sb);
        assertEquals(
                List.of(1, 2), t.getStops().stream().map(TourStop::getVisitOrder).toList());
        assertEquals(
                List.of(a, c), t.getStops().stream().map(TourStop::getCollectionPoint).toList());
    }

    @Test
    void capacityEqualIsWithinAndExcessIsReported() {
        Tour t = newTour();
        t.addStop(point("A", "30", "20"));
        t.assign(vehicle("50.00"), null, List.of());
        assertFalse(t.isOverCapacity());
        t.assign(vehicle("49.99"), null, List.of());
        assertTrue(t.isOverCapacity());
        List<TourIssue> issues = t.structuralIssues();
        assertEquals(TourIssueCode.OVER_CAPACITY, issues.get(issues.size() - 1).code());
        t.assign(null, null, List.of());
        assertFalse(t.isOverCapacity());
    }

    @Test
    void completeAvailableDraftHasNoIssues() {
        Tour t = newTour();
        t.addStop(point("A", "10"));
        t.assign(vehicle("100"), agent("Dana"), List.of(agent("Eli"), agent("Finn")));
        assertTrue(t.structuralIssues().isEmpty());
    }

    @Test
    void unavailableResourcesAreReported() {
        Tour t = newTour();
        t.addStop(point("A", "10"));
        Vehicle v = vehicle("100");
        Agent d = agent("Dana");
        Agent e = agent("Eli");
        t.assign(v, d, List.of(e, agent("Finn")));
        v.setStatus(ResourceStatus.UNAVAILABLE);
        d.setStatus(ResourceStatus.UNAVAILABLE);
        e.setStatus(ResourceStatus.UNAVAILABLE);
        List<TourIssueCode> codes = codes(t);
        assertTrue(codes.contains(TourIssueCode.VEHICLE_UNAVAILABLE));
        assertTrue(codes.contains(TourIssueCode.DRIVER_UNAVAILABLE));
        assertEquals(
                1, codes.stream().filter(c -> c == TourIssueCode.COLLECTOR_UNAVAILABLE).count());
    }

    @Test
    void inactiveOrIneligibleStopIsReported() {
        Tour t = newTour();
        CollectionPoint a = point("Alpha", "10");
        CollectionPoint b = point("Bravo", "10");
        t.addStop(a);
        t.addStop(b);
        a.setStatus(CollectionPointStatus.INACTIVE);
        b.setSourceOwnershipType("BID-owned Compactor");
        List<TourIssue> issues = t.structuralIssues();
        TourIssue inactive =
                issues.stream().filter(i -> i.code() == TourIssueCode.INACTIVE_STOP).findFirst().orElseThrow();
        TourIssue ineligible =
                issues.stream().filter(i -> i.code() == TourIssueCode.INELIGIBLE_STOP).findFirst().orElseThrow();
        assertTrue(inactive.message().contains("Stop 1") && inactive.message().contains("Alpha"));
        assertTrue(ineligible.message().contains("Stop 2") && ineligible.message().contains("Bravo"));
    }

    @Test
    void touchIncrementsRevision() {
        Tour t = newTour();
        t.touch();
        t.touch();
        assertEquals(2, t.getRevision());
        assertFalse(t.getUpdatedAt().isBefore(t.getCreatedAt()));
    }
}
