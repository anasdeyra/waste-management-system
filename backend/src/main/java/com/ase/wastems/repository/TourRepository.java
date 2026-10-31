package com.ase.wastems.repository;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access to {@link Tour} records. */
public interface TourRepository extends JpaRepository<Tour, Long> {

    List<Tour> findByPlannedDate(LocalDate plannedDate);

    List<Tour> findByStatus(TourStatus status);

    List<Tour> findAllByOrderByPlannedDateDesc();
}
