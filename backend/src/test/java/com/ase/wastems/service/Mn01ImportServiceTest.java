package com.ase.wastems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.config.DevMapDataSeeder;
import com.ase.wastems.config.ServiceAreaProperties;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.SourceSnapshot;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Tests of the idempotent MN01 import against the included real snapshots. */
@SpringBootTest
@ActiveProfiles("test")
class Mn01ImportServiceTest {

    private static final String BASKET_SHA =
            "a0737237e370d73152ee0eaec064385232c4c3027d7939d5a9033abf1b5a3013";
    private static final String DEPOT_SHA =
            "207851c2e229df0827a5132d2f3c6154934540daa1134b3a430e5b0a71c90832";

    @Autowired private Mn01ImportService service;
    @Autowired private CollectionPointService collectionPointService;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private SourceSnapshotRepository snapshots;
    @Autowired private ServiceAreaProperties area;

    @BeforeEach
    void setUp() {
        clean();
        categories.deleteAll();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        points.deleteAll();
        depots.deleteAll();
        snapshots.deleteAll();
    }

    private CollectionPoint pointBySource(String basketId) {
        return points.findBySourceDataset(Mn01ImportService.BASKET_DATASET).stream()
                .filter(p -> basketId.equals(p.getSourceIdentifier()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void checksumsMatchTheDocumentedSnapshots() {
        Mn01ImportReport r = service.importAll(ImportTrigger.MANUAL);
        assertEquals(BASKET_SHA, r.baskets().sha256());
        assertEquals(DEPOT_SHA, r.depots().sha256());
        assertEquals("8znf-7b2c:449:a0737237e370", r.baskets().snapshotId());
        assertEquals("xw3j-2yxf:1:207851c2e229", r.depots().snapshotId());
    }

    @Test
    void firstImportLoadsAllBasketsAndTheDepot() {
        Mn01ImportReport r = service.importAll(ImportTrigger.MANUAL);
        DatasetImportResult b = r.baskets();
        assertEquals(449, b.rowCount());
        assertEquals(238, b.dsnyEligibleRowCount());
        assertEquals(449, b.created());
        assertEquals(0, b.existing());
        assertEquals(0, b.updated());
        assertEquals(0, b.skipped());
        assertEquals(1, r.depots().created());
        assertEquals(449, points.count());
        List<Depot> all = depots.findAll();
        assertEquals(1, all.size());
        Depot d = all.get(0);
        assertEquals("MN01G Garage, 353 Spring St", d.getName());
        assertEquals(-74.0104183829902, d.getLongitude());
        assertEquals(40.7255225360961, d.getLatitude());
        assertEquals(DataOrigin.IMPORTED_REAL, d.getOrigin());
        assertEquals(238, collectionPointService.findEligibleForPlanning().size());
        for (CollectionPoint p : points.search(null, null)) {
            assertEquals(DataOrigin.IMPORTED_REAL, p.getOrigin());
            assertEquals(1, p.getDemands().size());
            DemandEntry de = p.getDemands().get(0);
            assertEquals(DataOrigin.SYNTHETIC, de.getOrigin());
            assertEquals("Mixed litter", de.getCategory().getName());
            assertTrue(de.getQuantityKg().compareTo(BigDecimal.TEN) >= 0);
            assertTrue(de.getQuantityKg().compareTo(new BigDecimal("30")) <= 0);
        }
        assertEquals(449, r.syntheticDemandsCreated());
        assertEquals(1, categories.count());
    }

    @Test
    void secondImportCreatesNothing() {
        service.importAll(ImportTrigger.MANUAL);
        Map<String, SourceSnapshot> before = snapshotsByDataset();
        BigDecimal demandBefore = totalDemand();
        Mn01ImportReport second = service.importAll(ImportTrigger.MANUAL);
        assertEquals(0, second.baskets().created());
        assertEquals(449, second.baskets().existing());
        assertEquals(0, second.baskets().updated());
        assertEquals(0, second.syntheticDemandsCreated());
        assertEquals(449, points.count());
        assertEquals(0, demandBefore.compareTo(totalDemand()));
        assertEquals(2, snapshots.count());
        Map<String, SourceSnapshot> after = snapshotsByDataset();
        for (String ds : before.keySet()) {
            assertEquals(before.get(ds).getImportedAt(), after.get(ds).getImportedAt());
            assertEquals(before.get(ds).getSnapshotId(), after.get(ds).getSnapshotId());
        }
    }

    private Map<String, SourceSnapshot> snapshotsByDataset() {
        return snapshots.findAll().stream()
                .collect(Collectors.toMap(SourceSnapshot::getSourceDataset, s -> s));
    }

    private BigDecimal totalDemand() {
        return points.search(null, null).stream()
                .map(CollectionPoint::totalDemandKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void devSampleIsAbsorbedWithoutDuplicates() {
        new DevMapDataSeeder(categories, depots, points).run(null);
        Mn01ImportReport r = service.importAll(ImportTrigger.STARTUP);
        assertEquals(444, r.baskets().created());
        assertEquals(5, r.baskets().existing());
        assertEquals(0, r.depots().created());
        assertEquals(1, r.depots().existing());
        assertEquals(449, points.count());
        CollectionPoint sample = pointBySource("10130067");
        assertEquals(2, sample.getDemands().size());
        assertEquals(
                0,
                sample.getDemands().stream()
                        .filter(d -> d.getCategory().getName().equals("Mixed litter"))
                        .findFirst()
                        .orElseThrow()
                        .getQuantityKg()
                        .compareTo(new BigDecimal("30")));
        assertEquals(
                0,
                sample.getDemands().stream()
                        .filter(d -> d.getCategory().getName().equals("Recyclables"))
                        .findFirst()
                        .orElseThrow()
                        .getQuantityKg()
                        .compareTo(new BigDecimal("12")));
        assertEquals(CollectionPointStatus.INACTIVE, pointBySource("10130345").getStatus());
        assertEquals(
                1,
                categories.findAll().stream()
                        .filter(c -> c.getName().equalsIgnoreCase("Mixed litter"))
                        .count());
        assertEquals(237, collectionPointService.findEligibleForPlanning().size());
    }

    @Test
    void localEditsSurviveReimport() {
        service.importAll(ImportTrigger.MANUAL);
        CollectionPoint p = pointBySource("10110163");
        java.util.UUID id = p.getId();
        java.util.UUID demandId = p.getDemands().get(0).getId();
        collectionPointService.update(id, "Renamed", -74.0, 40.71);
        collectionPointService.setStatus(id, CollectionPointStatus.INACTIVE);
        collectionPointService.updateDemand(
                id, demandId, p.getDemands().get(0).getCategory().getId(), new BigDecimal("99"));
        service.importAll(ImportTrigger.MANUAL);
        CollectionPoint after = pointBySource("10110163");
        assertEquals("Renamed", after.getName());
        assertEquals(-74.0, after.getLongitude());
        assertEquals(40.71, after.getLatitude());
        assertEquals(CollectionPointStatus.INACTIVE, after.getStatus());
        assertEquals(1, after.getDemands().size());
        assertEquals(0, after.getDemands().get(0).getQuantityKg().compareTo(new BigDecimal("99")));
        assertEquals(DataOrigin.USER_ENTERED, after.getDemands().get(0).getOrigin());
    }

    @Test
    void nonDsnyLabelsArePreservedAndNotEligible() {
        service.importAll(ImportTrigger.MANUAL);
        CollectionPoint bid = pointBySource("10110163");
        assertEquals("BID-owned Compactor", bid.getSourceOwnershipType());
        assertFalse(bid.isOptimizationEligible());
        assertFalse(
                collectionPointService.findEligibleForPlanning().stream()
                        .anyMatch(p -> p.getId().equals(bid.getId())));
        assertEquals("FULTON STREET & RYDERS ALLEY", bid.getName());
        CollectionPoint better = pointBySource("10110239");
        assertTrue(better.isOptimizationEligible());
        assertEquals("BROADWAY", better.getName());
        assertEquals("WEST BROADWAY", pointBySource("10130268").getName());
    }

    @Test
    void sourceLabelIsRefreshedOnUpsert() {
        CollectionPoint p = new CollectionPoint("Kept name", -74.005, 40.708, DataOrigin.IMPORTED_REAL);
        p.setSource("8znf-7b2c", "10110163", "OLD LABEL");
        com.ase.wastems.domain.WasteCategory cat =
                categories.save(new com.ase.wastems.domain.WasteCategory("Temp", null));
        p.addDemand(new DemandEntry(cat, BigDecimal.ONE, DataOrigin.USER_ENTERED));
        points.save(p);
        Mn01ImportReport r = service.importAll(ImportTrigger.MANUAL);
        assertEquals(1, r.baskets().existing());
        assertEquals(1, r.baskets().updated());
        assertEquals(448, r.baskets().created());
        CollectionPoint after = pointBySource("10110163");
        assertEquals("BID-owned Compactor", after.getSourceOwnershipType());
        assertEquals("Kept name", after.getName());
        assertEquals(1, after.getDemands().size());
    }

    @Test
    void lastReportIsEmptyBeforeAnyImport() {
        Mn01ImportService fresh =
                new Mn01ImportService(points, depots, categories, snapshots, area);
        assertTrue(fresh.lastReport().isEmpty());
        fresh.importAll(ImportTrigger.MANUAL);
        assertTrue(fresh.lastReport().isPresent());
        assertEquals(ImportTrigger.MANUAL, fresh.lastReport().get().trigger());
        assertNotNull(fresh.lastReport().get().baskets());
    }

    @Test
    void helpers() {
        assertEquals("A ST & B ST", Mn01ImportService.basketName("A ST", "B ST", "1"));
        assertEquals("B ST", Mn01ImportService.basketName("", "B ST", "1"));
        assertEquals("Basket 7", Mn01ImportService.basketName(" ", "", "7"));
        BigDecimal kg = Mn01ImportService.syntheticDemandKg("10110163");
        assertEquals(kg, Mn01ImportService.syntheticDemandKg("10110163"));
        assertTrue(kg.compareTo(BigDecimal.TEN) >= 0 && kg.compareTo(new BigDecimal("30")) <= 0);
    }
}
