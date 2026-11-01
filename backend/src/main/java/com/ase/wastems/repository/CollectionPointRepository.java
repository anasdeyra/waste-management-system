package com.ase.wastems.repository;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence of {@link CollectionPoint}; reads fetch demands and categories eagerly. */
public interface CollectionPointRepository extends JpaRepository<CollectionPoint, UUID> {

    @EntityGraph(attributePaths = {"demands", "demands.category"})
    Optional<CollectionPoint> findWithDemandsById(UUID id);

    @Query(
            """
            select distinct p from CollectionPoint p
              left join fetch p.demands d
              left join fetch d.category
            where (:status is null or p.status = :status)
              and (:categoryId is null or exists (
                    select 1 from DemandEntry e where e.collectionPoint = p and e.category.id = :categoryId))
            order by p.name asc""")
    List<CollectionPoint> search(
            @Param("status") CollectionPointStatus status, @Param("categoryId") UUID categoryId);

    @EntityGraph(attributePaths = {"demands", "demands.category"})
    List<CollectionPoint> findBySourceDataset(String dataset);

    boolean existsBySourceDatasetAndSourceIdentifier(String dataset, String identifier);

    @EntityGraph(attributePaths = {"demands", "demands.category"})
    Optional<CollectionPoint> findWithDemandsBySourceDatasetAndSourceIdentifier(
            String dataset, String identifier);

    boolean existsBySourceDataset(String dataset);

    Optional<CollectionPoint> findBySourceDatasetAndSourceIdentifier(String dataset, String identifier);
}
