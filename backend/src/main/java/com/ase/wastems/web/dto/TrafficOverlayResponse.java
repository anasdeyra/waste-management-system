package com.ase.wastems.web.dto;

import com.ase.wastems.traffic.GeometrySource;
import com.ase.wastems.traffic.SpeedBand;
import com.ase.wastems.traffic.TrafficCounts;
import com.ase.wastems.traffic.TrafficLink;
import com.ase.wastems.traffic.TrafficLinkStatus;
import com.ase.wastems.traffic.TrafficOverlay;
import com.ase.wastems.traffic.TrafficOverlayService;
import com.ase.wastems.traffic.TrafficSnapshot;
import java.time.Instant;
import java.util.List;

/** Display-only NYC DOT traffic overlay; unavailable snapshots give zero counts and no links. */
public record TrafficOverlayResponse(boolean available, String unavailableReason, String snapshotId, String source,
        String sourceUrl, Instant retrievedAt, Instant observedAt, String observedAtLocal, String timeZone,
        Long ageMinutes, int staleAfterMinutes, TrafficCountsResponse counts, String coverageNote,
        String displayOnlyNote, String attribution, String disclaimer, List<TrafficLinkResponse> links) {

    public record TrafficCountsResponse(int total, int observed, int stale, int noData, int invalid) {}

    public record LineStringGeometry(String type, List<double[]> coordinates) {}

    public record TrafficLinkResponse(String linkId, String name, String borough, String owner,
            TrafficLinkStatus status, Double speedMph, Integer travelTimeS, SpeedBand speedBand, Instant dataAsOf,
            String dataAsOfLocal, GeometrySource geometrySource, boolean geometryTruncated,
            LineStringGeometry geometry) {}

    public static TrafficOverlayResponse from(TrafficOverlay o) {
        TrafficSnapshot s = o.snapshot();
        if (!o.available() || s == null) {
            return new TrafficOverlayResponse(false, o.unavailableReason(), null, null, null, null, null, null, null,
                    null, o.staleAfterMinutes(), new TrafficCountsResponse(0, 0, 0, 0, 0),
                    TrafficOverlayService.COVERAGE_NOTE, TrafficOverlayService.DISPLAY_ONLY_NOTE,
                    TrafficOverlayService.ATTRIBUTION, TrafficOverlayService.DISCLAIMER, List.of());
        }
        TrafficCounts c = s.counts();
        return new TrafficOverlayResponse(true, null, s.snapshotId(), s.source(), s.sourceUrl(), s.retrievedAt(),
                s.observedAt(), s.observedAtLocal(), s.timeZone(), o.ageMinutes(), o.staleAfterMinutes(),
                new TrafficCountsResponse(c.total(), c.observed(), c.stale(), c.noData(), c.invalid()),
                TrafficOverlayService.COVERAGE_NOTE, TrafficOverlayService.DISPLAY_ONLY_NOTE,
                TrafficOverlayService.ATTRIBUTION, TrafficOverlayService.DISCLAIMER,
                s.links().stream().map(TrafficOverlayResponse::link).toList());
    }

    private static TrafficLinkResponse link(TrafficLink l) {
        return new TrafficLinkResponse(l.linkId(), l.name(), l.borough(), l.owner(), l.status(), l.speedMph(),
                l.travelTimeS(), l.speedBand(), l.dataAsOf(), l.dataAsOfLocal(), l.geometrySource(),
                l.geometryTruncated(), new LineStringGeometry("LineString", l.coordinates()));
    }
}
