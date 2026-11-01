package com.ase.wastems.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.WasteCategory;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** Persistence tests of the tour aggregate. */
@DataJpaTest
@ActiveProfiles("test")
class TourRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private TourRepository tours;

    private Depot depot;
    private CollectionPoint pa;
    private CollectionPoint pb;
    private CollectionPoint pc;
    private Agent eli;
    private Agent finn;

    @BeforeEach
    void fixtures() {
        WasteCategory paper = em.persist(new WasteCategory("Paper", null));
        WasteCategory glass = em.persist(new WasteCategory("Glass", null));
        depot = em.persist(new Depot("D", -74.0, 40.7, DataOrigin.USER_ENTERED, null, null));
        pa = point("A", paper, glass);
        pb = point("B", paper, null);
        pc = point("C", paper, null);
        eli = em.persist(new Agent("Eli", DataOrigin.USER_ENTERED));
        finn = em.persist(new Agent("Finn", DataOrigin.USER_ENTERED));
    }

    private CollectionPoint point(String name, WasteCategory c1, WasteCategory c2) {
        CollectionPoint p = new CollectionPoint(name, -74.0, 40.71, DataOrigin.USER_ENTERED);
        p.addDemand(new DemandEntry(c1, new BigDecimal("10"), DataOrigin.USER_ENTERED));
        if (c2 != null) {
            p.addDemand(new DemandEntry(c2, new BigDecimal("5"), DataOrigin.USER_ENTERED));
        }
        return em.persist(p);
    }

    private Tour newTour(LocalDate date) {
        return new Tour(date, depot, DataOrigin.USER_ENTERED);
    }

    @Test
    void persistsOrderedStopsSnapshotsAndCollectors() {
        Tour t = em.persist(newTour(LocalDate.now()));
        TourStop sa = t.addStop(pa);
        TourStop sb = t.addStop(pb);
        TourStop sc = t.addStop(pc);
        em.flush();
        t.reorderStops(List.of(sc.getId(), sa.getId(), sb.getId()));
        t.assign(null, null, List.of(eli, finn));
        em.flush();
        em.clear();

        Tour found = tours.findById(t.getId()).orElseThrow();
        assertEquals(
                List.of("C", "A", "B"),
                found.getStops().stream().map(s -> s.getCollectionPoint().getName()).toList());
        assertEquals(
                List.of(1, 2, 3), found.getStops().stream().map(TourStop::getVisitOrder).toList());
        assertEquals(
                List.of("Eli", "Finn"),
                found.getCollectors().stream().map(Agent::getDisplayName).toList());
        TourStop a =
                found.getStops().stream()
                        .filter(s -> s.getCollectionPoint().getName().equals("A"))
                        .findFirst()
                        .orElseThrow();
        assertEquals(2, a.getStopDemands().size());
    }

    @Test
    void samePointTwiceViolatesUniqueConstraint() {
        Tour t = em.persist(newTour(LocalDate.now()));
        t.addStop(pa);
        t.addStop(pa);
        assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    void searchFiltersByStatusAndDate() {
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = LocalDate.now().plusDays(2);
        Tour t1 = em.persist(newTour(d1));
        Tour t2 = em.persist(newTour(d2));
        em.flush();
        assertEquals(List.of(t2, t1), tours.search(null, null));
        assertEquals(List.of(t1), tours.search(TourStatus.DRAFT, d1));
        assertEquals(0, tours.search(TourStatus.PLANNED, null).size());
    }
}
