package com.ase.wastems.repository;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ScenarioKind;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence of {@link EvaluationRun}. */
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {

    List<EvaluationRun> findByTourIdOrderByCreatedAtDesc(UUID tourId);

    long countByTourId(UUID tourId);

    List<EvaluationRun> findByBatchIdOrderByCreatedAtAscIdAsc(UUID batchId);

    @Query(
            value = """
            select new com.ase.wastems.repository.RunListRow(r.id, r.scenarioKind, r.batchId, r.scenarioKey,
                r.demandProfile, r.seed, r.repetition, r.tourId, r.pointCount, r.status, r.failureCode,
                r.createdAt, r.createdBy, r.roadGraphVersion, r.totalDemandKg, r.vehicleCapacityKg)
            from EvaluationRun r
            where (:kind is null or r.scenarioKind = :kind)
              and (:batchId is null or r.batchId = :batchId)
              and (:profile is null or r.demandProfile = :profile)
              and (:pointCount is null or r.pointCount = :pointCount)
              and (:seed is null or r.seed = :seed)
              and (:status is null or r.status = :status)
            order by r.createdAt desc, r.id desc""",
            countQuery = """
            select count(r) from EvaluationRun r
            where (:kind is null or r.scenarioKind = :kind)
              and (:batchId is null or r.batchId = :batchId)
              and (:profile is null or r.demandProfile = :profile)
              and (:pointCount is null or r.pointCount = :pointCount)
              and (:seed is null or r.seed = :seed)
              and (:status is null or r.status = :status)""")
    Page<RunListRow> searchRuns(
            @Param("kind") ScenarioKind kind,
            @Param("batchId") UUID batchId,
            @Param("profile") String profile,
            @Param("pointCount") Integer pointCount,
            @Param("seed") Long seed,
            @Param("status") EvaluationStatus status,
            Pageable pageable);

    @Query("""
            select new com.ase.wastems.repository.RunMethodRow(r.id, r.status, r.scenarioKey, r.demandProfile,
                r.pointCount, m.method, m.feasible, m.distanceM, m.durationS, m.executionNanos, m.coverageOk,
                m.capacityOk)
            from EvaluationRun r left join r.results m where r.id in :ids""")
    List<RunMethodRow> findMethodRows(@Param("ids") Collection<UUID> ids);

    @Query("""
            select new com.ase.wastems.repository.RunMethodRow(r.id, r.status, r.scenarioKey, r.demandProfile,
                r.pointCount, m.method, m.feasible, m.distanceM, m.durationS, m.executionNanos, m.coverageOk,
                m.capacityOk)
            from EvaluationRun r left join r.results m where r.batchId = :batchId""")
    List<RunMethodRow> findMethodRowsByBatch(@Param("batchId") UUID batchId);

    @Query("""
            select r.id from EvaluationRun r
            where (:kind is null or r.scenarioKind = :kind)
              and (:batchId is null or r.batchId = :batchId)
              and (:runId is null or r.id = :runId)
            order by r.createdAt asc, r.id asc""")
    List<UUID> findIdsForExport(
            @Param("kind") ScenarioKind kind, @Param("batchId") UUID batchId, @Param("runId") UUID runId);
}
