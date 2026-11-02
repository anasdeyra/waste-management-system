package com.ase.wastems.repository;

import com.ase.wastems.domain.EvaluationBatch;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link EvaluationBatch}. */
public interface EvaluationBatchRepository extends JpaRepository<EvaluationBatch, UUID> {

    List<EvaluationBatch> findAllByOrderByCreatedAtDescIdDesc();
}
