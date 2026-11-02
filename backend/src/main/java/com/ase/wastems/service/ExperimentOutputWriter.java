package com.ase.wastems.service;

import com.ase.wastems.domain.EvaluationBatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Writes the files of a finished experiment batch for the headless runner (D-050). */
@Service
public class ExperimentOutputWriter {

    private final EvaluationExportService exports;
    private final EvaluationExplorerService explorer;
    private final ObjectMapper objectMapper;

    public ExperimentOutputWriter(EvaluationExportService exports, EvaluationExplorerService explorer,
            ObjectMapper objectMapper) {
        this.exports = exports;
        this.explorer = explorer;
        this.objectMapper = objectMapper;
    }

    /** Creates {@code dir} if needed and writes results.csv, scenarios.csv and batch.json (UTF-8). */
    public void write(UUID batchId, Path dir) {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("results.csv"), exports.resultsCsv(null, batchId, null),
                    StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("scenarios.csv"), exports.scenariosCsv(batchId), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("batch.json"), batchJson(batchId) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write experiment output to " + dir, e);
        }
    }

    private String batchJson(UUID batchId) {
        EvaluationBatch b = explorer.batch(batchId);
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", b.getId().toString());
        doc.put("kind", b.getKind().name());
        doc.put("label", b.getLabel());
        doc.put("status", b.getStatus().name());
        doc.put("canonical", b.isCanonical());
        doc.put("parameters", b.getParameters());
        doc.put("demandProfiles", b.demandProfileList());
        doc.put("seeds", b.seedList());
        doc.put("sizes", b.sizeList());
        doc.put("repetitions", b.getRepetitions());
        doc.put("vehicleCapacityKg", b.getVehicleCapacityKg());
        doc.put("sourceSnapshotIds", b.sourceSnapshotIdList());
        doc.put("roadGraphVersion", b.getRoadGraphVersion());
        doc.put("routingEngine", b.getRoutingEngine());
        doc.put("environment", b.getEnvironment());
        doc.put("scenarioCount", b.getScenarioCount());
        doc.put("runCount", b.getRunCount());
        doc.put("completedRunCount", b.getCompletedRunCount());
        doc.put("failedRunCount", b.getFailedRunCount());
        doc.put("createdAt", b.getCreatedAt() == null ? null : b.getCreatedAt().toString());
        doc.put("createdBy", b.getCreatedBy());
        doc.put("finishedAt", b.getFinishedAt() == null ? null : b.getFinishedAt().toString());
        doc.put("aggregates", explorer.aggregates(batchId));
        doc.put("scenarioKeys", explorer.scenarioKeys(batchId));
        try {
            return objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize batch.json", e);
        }
    }
}
