package com.ase.wastems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Tests of {@link WasteCategoryService}. */
@SpringBootTest
@ActiveProfiles("test")
class WasteCategoryServiceTest {

    @Autowired private WasteCategoryService service;
    @Autowired private WasteCategoryRepository repository;
    @Autowired private CollectionPointRepository points;

    @BeforeEach
    void setUp() {
        points.deleteAll();
        repository.deleteAll();
    }

    @Test
    void deactivatedCategoryIsExcludedFromActiveQuery() {
        service.create("Paper", null);
        WasteCategory glass = service.create("Glass", null);
        service.setActive(glass.getId(), false);
        assertEquals(
                List.of("Paper"), service.findActive().stream().map(WasteCategory::getName).toList());
        assertEquals(2, service.list(false).size());
    }

    @Test
    void requireActiveRejectsInactiveCategory() {
        WasteCategory paper = service.create("Paper", null);
        assertEquals(paper.getId(), service.requireActive(paper.getId()).getId());
        service.setActive(paper.getId(), false);
        assertThrows(BusinessRuleException.class, () -> service.requireActive(paper.getId()));
    }

    @Test
    void updateKeepsIdentity() {
        WasteCategory c = service.create("Paper", "x");
        WasteCategory u = service.update(c.getId(), "Cardboard", "x");
        assertEquals(c.getId(), u.getId());
        assertEquals("Cardboard", service.get(c.getId()).getName());
        assertEquals(1, repository.count());
    }

    @Test
    void blankDescriptionStoredAsNull() {
        WasteCategory c = service.create("Paper", "   ");
        assertNull(service.get(c.getId()).getDescription());
    }
}
