package com.ase.wastems.service;

import java.time.Instant;
import java.util.List;

/** Outcome of one demonstration load (US-33). */
public record DemoLoadReport(
        DemoTrigger trigger,
        Instant loadedAt,
        DemoCount categories,
        DemoCount demandEntries,
        DemoCount vehicles,
        DemoCount accounts,
        DemoCount agents,
        DemoCount tours,
        List<DemoTourState> tourStates,
        List<String> warnings) {}
