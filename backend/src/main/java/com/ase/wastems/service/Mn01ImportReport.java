package com.ase.wastems.service;

import java.time.Instant;
import java.util.List;

/** Result of one MN01 import run over the basket and depot snapshots. */
public record Mn01ImportReport(
        ImportTrigger trigger,
        Instant startedAt,
        Instant finishedAt,
        DatasetImportResult baskets,
        DatasetImportResult depots,
        int syntheticDemandsCreated,
        String syntheticDemandCategory,
        List<String> warnings) {}
