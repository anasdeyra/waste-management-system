package com.ase.wastems.web;

import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.service.ExperimentService;
import com.ase.wastems.service.ExperimentSpec;
import com.ase.wastems.web.dto.EvaluationBatchResponse;
import com.ase.wastems.web.dto.ExperimentRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs a seeded MN01 experiment batch synchronously (ADMIN only through the /api/admin/** rule;
 * at most 180 runs). A missing body means the fixed 60-scenario experiment.
 */
@RestController
@RequestMapping("/api/admin/experiments")
public class ExperimentController {

    private final ExperimentService experiments;

    public ExperimentController(ExperimentService experiments) {
        this.experiments = experiments;
    }

    @PostMapping
    public ResponseEntity<EvaluationBatchResponse> run(
            @RequestBody(required = false) ExperimentRequest body, Authentication auth) {
        ExperimentSpec spec = body == null
                ? ExperimentSpec.canonical()
                : ExperimentSpec.of(body.profiles(), body.seeds(), body.sizes(), body.repetitions(), body.label());
        EvaluationBatch batch = experiments.run(spec, auth.getName());
        return ResponseEntity.created(URI.create("/api/evaluations/batches/" + batch.getId()))
                .body(EvaluationBatchResponse.from(batch));
    }
}
