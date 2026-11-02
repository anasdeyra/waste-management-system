package com.ase.wastems.repository;

import com.ase.wastems.domain.DemandEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of {@link DemandEntry} for batch operations. The aggregate root is the
 * {@link CollectionPointRepository}; this interface currently serves only the demonstration
 * reset of the demand entries recorded by the last demo load (D-055).
 */
public interface DemandEntryRepository extends JpaRepository<DemandEntry, UUID> {}
