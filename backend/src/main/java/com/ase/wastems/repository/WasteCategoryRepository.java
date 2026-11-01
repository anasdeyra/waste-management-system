package com.ase.wastems.repository;

import com.ase.wastems.domain.WasteCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link WasteCategory}. */
public interface WasteCategoryRepository extends JpaRepository<WasteCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Optional<WasteCategory> findByNameIgnoreCase(String name);

    List<WasteCategory> findAllByOrderByNameAsc();

    List<WasteCategory> findByActiveTrueOrderByNameAsc();
}
