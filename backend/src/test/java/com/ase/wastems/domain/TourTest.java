package com.ase.wastems.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.ase.wastems.optimization.PlanningMethod;
import org.springframework.test.util.ReflectionTestUtils;
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

    private void attach(Tour t) {
        t.attachRoute(UUID.randomUUID(), PlanningMethod.NN_PLUS_2_OPT, "osm-mn01-test",
                "{\"type\":\"FeatureCollection\",\"features\":[]}", 100.0, 700.0);
    }

    private Tour tourWithStops(int n) {
        Tour t = newTour();
        for (int i = 0; i < n; i++) {
            TourStop s = t.addStop(point("P" + i, "10"));
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
        }
        attach(t);
        return t;
    }

    private Tour readyTour(boolean withRoute) {
        Tour t = newTour();
        t.addStop(point("A", "10"));
        t.assign(vehicle("1000"), agent("Dana"), List.of());
        if (withRoute) {
            attach(t);
        }
        return t;
    }

    private List<TourIssueCode> confirmationCodes(Tour t) {
        return t.confirmationIssues(LocalDate.now()).stream().map(TourIssue::code).toList();
    }

    @Test
    void attachAndClearRoute() {
        Tour t = newTour();
        attach(t);
        assertTrue(t.hasRoute());
        assertNotNull(t.getRoadLegsGeoJson());
        assertEquals("osm-mn01-test", t.getRoadGraphVersion());
        assertNotNull(t.getRouteEvaluationRunId());
        assertEquals(PlanningMethod.NN_PLUS_2_OPT, t.getRouteMethod());
        assertEquals(100.0, t.getRouteDistanceM());
        assertEquals(700.0, t.getRouteDurationS());
        t.clearRoute();
        assertFalse(t.hasRoute());
        assertNull(t.getRoadLegsGeoJson());
        assertNull(t.getRoadGraphVersion());
        assertNull(t.getRouteEvaluationRunId());
        assertNull(t.getRouteMethod());
        assertNull(t.getRouteDistanceM());
        assertNull(t.getRouteDurationS());
    }

    @Test
    void stopChangesClearTheRoute() {
        Tour added = tourWithStops(2);
        added.addStop(point("X", "5"));
        assertFalse(added.hasRoute());

        Tour removed = tourWithStops(2);
        removed.removeStop(removed.getStops().get(0));
        assertFalse(removed.hasRoute());

        Tour reordered = tourWithStops(2);
        List<UUID> ids = reordered.getStops().stream().map(TourStop::getId).toList();
        reordered.reorderStops(List.of(ids.get(1), ids.get(0)));
        assertFalse(reordered.hasRoute());
    }

    @Test
    void depotChangeClearsRouteButSameDepotKeepsIt() {
        Tour t = tourWithStops(1);
        t.setDepot(depot);
        assertTrue(t.hasRoute());
        t.setDepot(new Depot("Other", -74.0, 40.72, DataOrigin.USER_ENTERED, null, null));
        assertFalse(t.hasRoute());
    }

    @Test
    void crewAndDateEditsKeepTheRoute() {
        Tour t = tourWithStops(1);
        t.assign(vehicle("100"), agent("Dana"), List.of(agent("Eli")));
        t.setPlanningDate(LocalDate.now().plusDays(2));
        assertTrue(t.hasRoute());
    }

    @Test
    void confirmationIssuesOrder() {
        Tour t = newTour();
        t.addStop(point("A", "50"));
        t.assign(vehicle("10"), agent("Dana"), List.of());
        t.setPlanningDate(LocalDate.now().minusDays(1));
        assertEquals(
                List.of(TourIssueCode.OVER_CAPACITY, TourIssueCode.PLANNING_DATE_PAST, TourIssueCode.ROUTE_MISSING),
                confirmationCodes(t));
    }

    @Test
    void readyDraftHasNoConfirmationIssue() {
        assertTrue(confirmationCodes(readyTour(true)).isEmpty());
        Tour t = readyTour(false);
        assertEquals(List.of(TourIssueCode.ROUTE_MISSING), confirmationCodes(t));
        assertTrue(t.confirmationIssues(LocalDate.now()).get(0).message().contains("Evaluate the tour"));
    }

    @Test
    void confirmMovesDraftToPlanned() {
        Tour t = readyTour(true);
        t.confirm();
        assertEquals(TourStatus.PLANNED, t.getStatus());
        assertTrue(confirmationCodes(t).isEmpty());
        assertThrows(IllegalStateException.class, t::confirm);
    }

    @Test
    void confirmWithoutRouteIsRefused() {
        Tour t = readyTour(false);
        assertThrows(IllegalStateException.class, t::confirm);
        assertEquals(TourStatus.DRAFT, t.getStatus());
    }

    private CollectionPoint pointA;
    private Agent dana;
    private Agent eli;
    private final AppUser actor = new AppUser("agent1", "x", AppRole.AGENT);
    private final Instant now = Instant.now();

    private Tour plannedTour() {
        Tour t = newTour();
        pointA = point("A", "10", "5");
        t.addStop(pointA);
        t.addStop(point("B", "7"));
        dana = agent("Dana");
        eli = agent("Eli");
        ReflectionTestUtils.setField(dana, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(eli, "id", UUID.randomUUID());
        t.assign(vehicle("1000"), dana, List.of(eli));
        for (TourStop s : t.getStops()) {
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
        }
        attach(t);
        t.confirm();
        return t;
    }

    private Tour startedTour() {
        Tour t = plannedTour();
        t.start(now, "agent1");
        return t;
    }

    private List<TourIssueCode> startCodes(Tour t) {
        return t.startIssues().stream().map(TourIssue::code).toList();
    }

    @Test
    void startIssuesEmptyUnlessPlanned() {
        assertTrue(readyTour(true).startIssues().isEmpty());
        Tour t = plannedTour();
        assertTrue(t.startIssues().isEmpty());
        t.start(now, "agent1");
        assertTrue(t.startIssues().isEmpty());
    }

    @Test
    void startIssuesReportStructuralProblemsThenRoute() {
        Tour t = plannedTour();
        t.getVehicle().setStatus(ResourceStatus.UNAVAILABLE);
        assertEquals(List.of(TourIssueCode.VEHICLE_UNAVAILABLE), startCodes(t));
        pointA.setStatus(CollectionPointStatus.INACTIVE);
        assertEquals(List.of(TourIssueCode.VEHICLE_UNAVAILABLE, TourIssueCode.INACTIVE_STOP), startCodes(t));
        t.clearRoute();
        List<TourIssueCode> codes = startCodes(t);
        assertEquals(TourIssueCode.ROUTE_MISSING, codes.get(codes.size() - 1));
    }

    @Test
    void startMovesPlannedToInProgress() {
        Tour t = plannedTour();
        t.start(now, "agent1");
        assertEquals(TourStatus.IN_PROGRESS, t.getStatus());
        assertEquals(now, t.getStartedAt());
        assertEquals("agent1", t.getStartedBy());
        assertTrue(t.getStops().stream().allMatch(s -> s.getStatus() == TourStopStatus.PENDING));
        assertTrue(t.hasRoute());
        assertThrows(IllegalStateException.class, () -> t.start(now, "agent1"));
        assertThrows(IllegalStateException.class, () -> readyTour(true).start(now, "agent1"));
    }

    @Test
    void isCrewMemberMatchesDriverAndCollectors() {
        Tour t = plannedTour();
        assertTrue(t.isCrewMember(dana.getId()));
        assertTrue(t.isCrewMember(eli.getId()));
        assertFalse(t.isCrewMember(UUID.randomUUID()));
        assertFalse(t.isCrewMember(null));
    }

    @Test
    void recordCollectedStoresQuantitiesActorAndTime() {
        Tour t = startedTour();
        Map<WasteCategory, BigDecimal> q = new LinkedHashMap<>();
        q.put(paper, new BigDecimal("10.00"));
        q.put(glass, new BigDecimal("0.00"));
        TourStop s = t.getStops().get(0);
        s.recordCollected(q, "  lid broken ", actor, now);
        assertEquals(TourStopStatus.COLLECTED, s.getStatus());
        assertEquals(2, s.getActualQuantities().size());
        assertEquals(paper, s.getActualQuantities().get(0).getCategory());
        assertEquals(glass, s.getActualQuantities().get(1).getCategory());
        assertEquals(0, s.actualTotalKg().compareTo(new BigDecimal("10.00")));
        assertEquals("lid broken", s.getIncidentReason());
        assertEquals(actor, s.getOutcomeRecordedBy());
        assertEquals(now, s.getOutcomeRecordedAt());
        assertTrue(s.isTerminal());
    }

    @Test
    void recordNotCollectedRequiresReason() {
        Tour t = startedTour();
        TourStop s = t.getStops().get(0);
        assertThrows(IllegalArgumentException.class, () -> s.recordNotCollected("  ", actor, now));
        assertEquals(TourStopStatus.PENDING, s.getStatus());
        s.recordNotCollected("Blocked", actor, now);
        assertEquals(TourStopStatus.NOT_COLLECTED, s.getStatus());
        assertTrue(s.getActualQuantities().isEmpty());
        assertEquals("Blocked", s.getIncidentReason());
    }

    @Test
    void outcomeGuards() {
        Map<WasteCategory, BigDecimal> ok = Map.of(paper, new BigDecimal("1"));
        Tour planned = plannedTour();
        TourStop onPlanned = planned.getStops().get(0);
        assertThrows(IllegalStateException.class, () -> onPlanned.recordCollected(ok, null, actor, now));
        Tour t = startedTour();
        TourStop s = t.getStops().get(0);
        s.recordCollected(ok, null, actor, now);
        assertThrows(IllegalStateException.class, () -> s.recordCollected(ok, null, actor, now));
        TourStop s2 = t.getStops().get(1);
        assertThrows(IllegalArgumentException.class,
                () -> s2.recordCollected(Map.of(paper, new BigDecimal("-1")), null, actor, now));
        assertThrows(IllegalArgumentException.class, () -> s2.recordCollected(Map.of(), null, actor, now));
    }

    @Test
    void completionIssuesListPendingStops() {
        Tour t = startedTour();
        Map<WasteCategory, BigDecimal> ok = Map.of(paper, new BigDecimal("3"));
        t.getStops().get(0).recordCollected(ok, null, actor, now);
        List<TourIssue> issues = t.completionIssues();
        assertEquals(1, issues.size());
        assertEquals(TourIssueCode.STOP_PENDING, issues.get(0).code());
        assertTrue(issues.get(0).message().contains("Stop 2 (B)"));
        assertThrows(IllegalStateException.class, () -> t.complete(now, "agent2"));
        t.getStops().get(1).recordNotCollected("Blocked", actor, now);
        assertTrue(t.completionIssues().isEmpty());
        t.complete(now, "agent2");
        assertEquals(TourStatus.COMPLETED, t.getStatus());
        assertEquals("agent2", t.getCompletedBy());
        assertEquals(now, t.getCompletedAt());
        assertTrue(t.hasRoute());
        assertTrue(t.isCrewMember(dana.getId()));
        assertEquals(2, t.terminalStopCount());
        assertThrows(IllegalStateException.class, () -> t.complete(now, "agent2"));
    }

    @Test
    void terminalStopCountCountsOutcomes() {
        Tour t = startedTour();
        assertEquals(0, t.terminalStopCount());
        t.getStops().get(0).recordNotCollected("Blocked", actor, now);
        assertEquals(1, t.terminalStopCount());
    }
}
