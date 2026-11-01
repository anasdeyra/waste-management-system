package com.ase.wastems.repository;

import com.ase.wastems.domain.Depot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Depot}. */
public interface DepotRepository extends JpaRepository<Depot, UUID> {

    List<Depot> findAllByOrderByNameAsc();

    Optional<Depot> findBySourceDatasetAndSourceIdentifier(String dataset, String identifier);
}
