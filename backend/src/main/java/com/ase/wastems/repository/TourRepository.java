package com.ase.wastems.repository;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import java.time.LocalDate;
import java.util.Collection;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence of {@link Tour}. */
public interface TourRepository extends JpaRepository<Tour, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tour t where t.id = :id")
    Optional<Tour> findByIdForUpdate(@Param("id") UUID id);

    @Query(
            """
            select t from Tour t
            where (:status is null or t.status = :status)
              and (:planningDate is null or t.planningDate = :planningDate)
            order by t.planningDate desc, t.createdAt desc""")
    List<Tour> search(
            @Param("status") TourStatus status, @Param("planningDate") LocalDate planningDate);

    List<Tour> findByStatusIn(Collection<TourStatus> statuses);

    @Query(
            """
            select distinct t from Tour t
              left join t.driver d
              left join t.collectors c
            where t.status <> com.ase.wastems.domain.TourStatus.DRAFT
              and (t.cancelledFromStatus is null
                   or t.cancelledFromStatus <> com.ase.wastems.domain.TourStatus.DRAFT)
              and (d.id = :agentId or c.id = :agentId)""")
    List<Tour> findAssignedTo(@Param("agentId") UUID agentId);
}
