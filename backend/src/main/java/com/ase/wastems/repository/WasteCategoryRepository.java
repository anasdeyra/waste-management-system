package com.ase.wastems.repository;

import com.ase.wastems.domain.WasteCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access to {@link WasteCategory} records. */
public interface WasteCategoryRepository extends JpaRepository<WasteCategory, Long> {

    List<WasteCategory> findAllByOrderByNameAsc();
}
