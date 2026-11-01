package com.ase.wastems.repository;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import java.time.LocalDate;
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
}
