package com.ase.wastems.service;

import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.repository.EvaluationBatchRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.RunListRow;
import com.ase.wastems.repository.RunMethodRow;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only queries of the evaluation explorer (US-36): run lists, batches and their aggregates. */
@Service
public class EvaluationExplorerService {

    private final EvaluationRunRepository runs;
    private final EvaluationBatchRepository batches;

    public EvaluationExplorerService(EvaluationRunRepository runs, EvaluationBatchRepository batches) {
        this.runs = runs;
        this.batches = batches;
    }

    @Transactional(readOnly = true)
    public Page<RunListRow> searchRuns(RunFilter f, int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("page", "must be at least 0");
        }
        if (size < 1 || size > 200) {
            throw new InvalidRequestException("size", "must be between 1 and 200");
        }
        if (f.pointCount() != null && f.pointCount() < 1) {
            throw new InvalidRequestException("pointCount", "must be at least 1");
        }
        return runs.searchRuns(f.kind(), f.batchId(), f.profile(), f.pointCount(), f.seed(), f.status(),
                PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<RunMethodRow>> methodRows(Collection<UUID> runIds) {
        if (runIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<RunMethodRow>> map = new HashMap<>();
        for (RunMethodRow r : runs.findMethodRows(runIds)) {
            map.computeIfAbsent(r.runId(), k -> new java.util.ArrayList<>()).add(r);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<EvaluationBatch> batches() {
        return batches.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public EvaluationBatch batch(UUID id) {
        return batches.findById(id).orElseThrow(() -> new NotFoundException("Evaluation batch not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AggregateRow> aggregates(UUID batchId) {
        return EvaluationAggregates.compute(runs.findMethodRowsByBatch(batchId));
    }

    @Transactional(readOnly = true)
    public List<String> scenarioKeys(UUID batchId) {
        return runs.findMethodRowsByBatch(batchId).stream().map(RunMethodRow::scenarioKey)
                .filter(java.util.Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }
}
