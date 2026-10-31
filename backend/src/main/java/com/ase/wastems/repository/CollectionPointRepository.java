package com.ase.wastems.repository;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access to {@link CollectionPoint} records. */
public interface CollectionPointRepository extends JpaRepository<CollectionPoint, Long> {

    List<CollectionPoint> findByStatus(CollectionPointStatus status);

    List<CollectionPoint> findAllByOrderByNameAsc();
}
