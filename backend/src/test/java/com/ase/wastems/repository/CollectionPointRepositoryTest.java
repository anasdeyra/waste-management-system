package com.ase.wastems.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.WasteCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CollectionPointRepositoryTest {

    @Autowired private CollectionPointRepository points;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private DepotRepository depots;
    @Autowired private TestEntityManager em;

    private WasteCategory paper() {
        return categories.save(new WasteCategory("Paper", null));
    }

    private WasteCategory glass() {
        return categories.save(new WasteCategory("Glass", null));
    }

    private CollectionPoint point(String name, WasteCategory c, CollectionPointStatus status) {
        CollectionPoint p = new CollectionPoint(name, -74.0, 40.71, DataOrigin.USER_ENTERED);
        p.setStatus(status);
        p.addDemand(new DemandEntry(c, new BigDecimal("10"), DataOrigin.USER_ENTERED));
        return p;
    }

    @Test
    void roundTripCascadesDemands() {
        WasteCategory paper = paper();
        WasteCategory glass = glass();
        CollectionPoint p = new CollectionPoint("A", -74.0, 40.71, DataOrigin.SYNTHETIC);
        p.addDemand(new DemandEntry(paper, new BigDecimal("10.50"), DataOrigin.USER_ENTERED));
        p.addDemand(new DemandEntry(glass, new BigDecimal("4.25"), DataOrigin.USER_ENTERED));
        UUID id = points.save(p).getId();
        em.flush();
        em.clear();

        CollectionPoint loaded = points.findWithDemandsById(id).orElseThrow();
        assertEquals(2, loaded.getDemands().size());
        assertTrue(loaded.getDemands().stream().anyMatch(d -> d.getCategory().getName().equals("Paper")));
        assertTrue(loaded.getDemands().stream().anyMatch(d -> d.getCategory().getName().equals("Glass")));
        assertEquals(CollectionPointStatus.ACTIVE, loaded.getStatus());
        assertEquals(DataOrigin.SYNTHETIC, loaded.getOrigin());
        assertEquals(0, loaded.totalDemandKg().compareTo(new BigDecimal("14.75")));
    }

    @Test
    void removingDemandDeletesOrphan() {
        CollectionPoint p = new CollectionPoint("A", -74.0, 40.71, DataOrigin.USER_ENTERED);
        p.addDemand(new DemandEntry(paper(), BigDecimal.ONE, DataOrigin.USER_ENTERED));
        p.addDemand(new DemandEntry(glass(), BigDecimal.ONE, DataOrigin.USER_ENTERED));
        UUID id = points.save(p).getId();
        em.flush();
        em.clear();

        CollectionPoint loaded = points.findWithDemandsById(id).orElseThrow();
        loaded.removeDemand(loaded.getDemands().get(0));
        em.flush();
        em.clear();

        assertEquals(1, points.findWithDemandsById(id).orElseThrow().getDemands().size());
        Long count = em.getEntityManager().createQuery("select count(d) from DemandEntry d", Long.class)
                .getSingleResult();
        assertEquals(1L, count);
    }

    @Test
    void rejectsDuplicateSourceKey() {
        WasteCategory c = paper();
        CollectionPoint a = point("A", c, CollectionPointStatus.ACTIVE);
        a.setSource("8znf-7b2c", "1", "DSNY PSR");
        points.saveAndFlush(a);
        CollectionPoint b = point("B", c, CollectionPointStatus.ACTIVE);
        b.setSource("8znf-7b2c", "1", "DSNY PSR");
        assertThrows(DataIntegrityViolationException.class, () -> points.saveAndFlush(b));
    }

    @Test
    void allowsManyPointsWithoutSource() {
        WasteCategory c = paper();
        points.saveAndFlush(point("A", c, CollectionPointStatus.ACTIVE));
        points.saveAndFlush(point("B", c, CollectionPointStatus.ACTIVE));
        assertEquals(2, points.count());
    }

    @Test
    void rejectsDuplicateCategoryOnPoint() {
        WasteCategory c = paper();
        CollectionPoint p = point("A", c, CollectionPointStatus.ACTIVE);
        p.addDemand(new DemandEntry(c, new BigDecimal("3"), DataOrigin.USER_ENTERED));
        assertThrows(DataIntegrityViolationException.class, () -> points.saveAndFlush(p));
    }

    @Test
    void searchFiltersByStatusAndCategory() {
        WasteCategory paper = paper();
        WasteCategory glass = glass();
        points.save(point("A", paper, CollectionPointStatus.ACTIVE));
        points.save(point("B", glass, CollectionPointStatus.ACTIVE));
        points.save(point("C", paper, CollectionPointStatus.INACTIVE));
        em.flush();
        em.clear();

        assertEquals(List.of("A", "B", "C"), names(points.search(null, null)));
        assertEquals(List.of("A", "B"), names(points.search(CollectionPointStatus.ACTIVE, null)));
        assertEquals(List.of("C"), names(points.search(CollectionPointStatus.INACTIVE, null)));
        assertEquals(List.of("A", "C"), names(points.search(null, paper.getId())));
        assertEquals(List.of("A"), names(points.search(CollectionPointStatus.ACTIVE, paper.getId())));
        points.search(null, null)
                .forEach(p -> assertTrue(Hibernate.isInitialized(p.getDemands())));
    }

    private static List<String> names(List<CollectionPoint> list) {
        return list.stream().map(CollectionPoint::getName).toList();
    }

    @Test
    void depotRoundTrip() {
        depots.saveAndFlush(
                new Depot(
                        "MN01G Garage",
                        -74.0104183829902,
                        40.7255225360961,
                        DataOrigin.IMPORTED_REAL,
                        "xw3j-2yxf",
                        "MN01G"));
        em.clear();
        Depot d = depots.findBySourceDatasetAndSourceIdentifier("xw3j-2yxf", "MN01G").orElseThrow();
        assertEquals(-74.0104183829902, d.getLongitude());
        assertEquals(40.7255225360961, d.getLatitude());
    }
}
