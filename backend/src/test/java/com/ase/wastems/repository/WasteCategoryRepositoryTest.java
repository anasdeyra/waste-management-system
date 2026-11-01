package com.ase.wastems.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ase.wastems.domain.WasteCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Persistence tests of {@link WasteCategoryRepository}. */
@DataJpaTest
@ActiveProfiles("test")
class WasteCategoryRepositoryTest {

    @Autowired private WasteCategoryRepository repository;

    @Test
    void roundTripKeepsDescription() {
        WasteCategory saved = repository.saveAndFlush(new WasteCategory("Paper", "Cardboard"));
        WasteCategory found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Cardboard", found.getDescription());
        assertTrue(found.isActive());
    }

    @Test
    void rejectsDuplicateName() {
        repository.saveAndFlush(new WasteCategory("Paper", null));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new WasteCategory("Paper", null)));
    }

    @Test
    void activeQueryReturnsOnlyActiveInNameOrder() {
        repository.save(new WasteCategory("Paper", null));
        WasteCategory glass = new WasteCategory("Glass", null);
        glass.setActive(false);
        repository.save(glass);
        repository.saveAndFlush(new WasteCategory("Metal", null));
        List<String> names =
                repository.findByActiveTrueOrderByNameAsc().stream()
                        .map(WasteCategory::getName)
                        .toList();
        assertEquals(List.of("Metal", "Paper"), names);
    }

    @Test
    void existsIgnoresCase() {
        repository.saveAndFlush(new WasteCategory("Paper", null));
        assertTrue(repository.existsByNameIgnoreCase("paper"));
    }
}
