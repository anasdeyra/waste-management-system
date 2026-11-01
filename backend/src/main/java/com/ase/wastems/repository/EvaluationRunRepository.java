package com.ase.wastems.repository;

import com.ase.wastems.domain.EvaluationRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link EvaluationRun}. */
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {

    List<EvaluationRun> findByTourIdOrderByCreatedAtDesc(UUID tourId);

    long countByTourId(UUID tourId);
}
