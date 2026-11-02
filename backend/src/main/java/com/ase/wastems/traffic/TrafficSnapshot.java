package com.ase.wastems.traffic;

import java.time.Instant;
import java.util.List;

/** A parsed, classified NYC DOT traffic snapshot (display only). */
public record TrafficSnapshot(String snapshotId, String source, String sourceUrl, Instant retrievedAt,
        Instant observedAt, String observedAtLocal, String timeZone, TrafficCounts counts, List<TrafficLink> links) {}
