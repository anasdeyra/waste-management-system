package com.ase.wastems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Tests of {@link CollectionPointService}. */
@SpringBootTest
@ActiveProfiles("test")
class CollectionPointServiceTest {

    @Autowired private CollectionPointService service;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;

    private WasteCategory paper;
    private WasteCategory glass;

    @BeforeEach
    void setUp() {
        points.deleteAll();
        depots.deleteAll();
        categories.deleteAll();
        agents.deleteAll();
        users.deleteAll();
        paper = categories.save(new WasteCategory("Paper", null));
        glass = categories.save(new WasteCategory("Glass", null));
    }

    private CollectionPoint make(String name) {
        return service.create(
                name, -74.0, 40.71, List.of(new DemandInput(paper.getId(), new BigDecimal("5"))));
    }

    @Test
    void findEligibleForPlanningExcludesInactive() {
        make("A");
        make("B");
        CollectionPoint c = make("C");
        service.setStatus(c.getId(), CollectionPointStatus.INACTIVE);
        List<String> names =
                service.findEligibleForPlanning().stream().map(CollectionPoint::getName).toList();
        assertEquals(List.of("A", "B"), names);
        assertFalse(names.contains("C"));
    }

    @Test
    void serviceAreaBoundsAreInclusive() {
        List<DemandInput> d = List.of(new DemandInput(paper.getId(), BigDecimal.ONE));
        service.create("Corner", -74.30, 40.45, d);
        assertThrows(BusinessRuleException.class, () -> service.create("Out", -74.31, 40.7, d));
    }

    @Test
    void totalDemandSumsMixedCategories() {
        CollectionPoint p =
                service.create(
                        "Mixed",
                        -74.0,
                        40.71,
                        List.of(
                                new DemandInput(paper.getId(), new BigDecimal("10.5")),
                                new DemandInput(glass.getId(), new BigDecimal("4.25"))));
        CollectionPoint loaded = service.get(p.getId());
        assertEquals(0, loaded.totalDemandKg().compareTo(new BigDecimal("14.75")));
    }

    @Test
    void eligibilityFollowsTheDsnyRule() {
        CollectionPoint userEntered = make("User");
        CollectionPoint dsny = importedPoint("Wire", "DSNY Wire");
        importedPoint("Private", "Private High-end");
        List<String> names =
                service.findEligibleForPlanning().stream().map(CollectionPoint::getName).toList();
        assertEquals(2, names.size());
        assertTrue(names.contains(userEntered.getName()));
        assertTrue(names.contains(dsny.getName()));
        assertFalse(names.contains("Private"));
    }

    private CollectionPoint importedPoint(String name, String label) {
        CollectionPoint p = new CollectionPoint(name, -74.0, 40.71, DataOrigin.IMPORTED_REAL);
        p.setSource("8znf-7b2c", name, label);
        p.addDemand(new DemandEntry(paper, BigDecimal.ONE, DataOrigin.SYNTHETIC));
        return points.save(p);
    }
}
