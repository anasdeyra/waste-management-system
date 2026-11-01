package com.ase.wastems.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** The dev map seed is idempotent and keeps real locations apart from synthetic demand. */
@DataJpaTest
@ActiveProfiles("test")
class DevMapDataSeederTest {

    @Autowired private WasteCategoryRepository categories;
    @Autowired private DepotRepository depots;
    @Autowired private CollectionPointRepository points;
    @Autowired private TestEntityManager em;

    private DevMapDataSeeder seeder() {
        return new DevMapDataSeeder(categories, depots, points);
    }

    @Test
    void seedingTwiceGivesIdenticalCounts() {
        seeder().run(null);
        seeder().run(null);
        em.flush();
        em.clear();
        assertEquals(2, categories.count());
        assertEquals(1, depots.count());
        assertEquals(5, points.count());

        List<CollectionPoint> all = points.search(null, null);
        assertEquals(4, all.stream().filter(p -> p.getStatus() == CollectionPointStatus.ACTIVE).count());
        assertEquals(1, all.stream().filter(p -> p.getStatus() == CollectionPointStatus.INACTIVE).count());
        for (CollectionPoint p : all) {
            assertEquals(DataOrigin.IMPORTED_REAL, p.getOrigin());
            assertEquals("8znf-7b2c", p.getSourceDataset());
            assertFalse(p.getSourceOwnershipType().isBlank());
            assertTrue(p.getSourceOwnershipType().startsWith("DSNY "));
            assertFalse(p.getDemands().isEmpty());
            p.getDemands().forEach(d -> assertEquals(DataOrigin.SYNTHETIC, d.getOrigin()));
        }
    }

    @Test
    void existingCategoryIsReusedIgnoringCase() {
        categories.saveAndFlush(new WasteCategory("mixed LITTER", null));
        seeder().run(null);
        assertEquals(2, categories.count());
        assertEquals(5, points.count());
    }
}
