package com.ase.wastems.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ase.wastems.config.ServiceAreaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TrafficSnapshotParserTest {

    static final ServiceAreaProperties AREA = new ServiceAreaProperties(-74.30, -73.65, 40.45, 40.95);
    static final ObjectMapper M = new ObjectMapper();
    static final String POLY = "uxmwFn_tbMgAwBi@eBc@sB[qC[wE";

    private final TrafficSnapshotParser parser = new TrafficSnapshotParser(AREA, 15, 100);

    private byte[] committed() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/traffic/mn01-traffic-links.json")) {
            return in.readAllBytes();
        }
    }

    private Map<String, TrafficLink> byId(TrafficSnapshot s) {
        return s.links().stream().collect(Collectors.toMap(TrafficLink::linkId, Function.identity()));
    }

    @Test
    void committedSnapshot() throws Exception {
        TrafficSnapshot s = parser.parse(committed());
        assertThat(s.snapshotId()).isEqualTo("nyc-dot-i4gi-tjb9:12:2a1b9f1faa7f");
        assertThat(s.observedAt()).isEqualTo(Instant.parse("2026-11-01T20:08:05Z"));
        assertThat(s.observedAtLocal()).isEqualTo("2026-11-01T15:08:05");
        assertThat(s.timeZone()).isEqualTo("America/New_York");
        assertThat(s.retrievedAt()).isEqualTo(Instant.parse("2026-11-01T21:53:38Z"));
        assertThat(s.counts()).isEqualTo(new TrafficCounts(12, 8, 0, 4, 0));
        assertThat(s.links()).hasSize(12);
        Map<String, TrafficLink> l = byId(s);
        for (String id : new String[] {"4456494", "4616339"}) {
            assertThat(l.get(id)).isNotNull();
        }
        assertThat(l.get("4456494").status()).isEqualTo(TrafficLinkStatus.OBSERVED);
        assertThat(l.get("4456494").speedBand()).isEqualTo(SpeedBand.BELOW_10_MPH);
        assertThat(l.get("4616340").speedBand()).isEqualTo(SpeedBand.BELOW_10_MPH);
        assertThat(l.get("4616339").speedBand()).isEqualTo(SpeedBand.FROM_10_TO_25_MPH);
        assertThat(l.get("4616342").speedBand()).isEqualTo(SpeedBand.FROM_10_TO_25_MPH);
        for (String id : new String[] {"4616257", "4616271", "4616328", "4616333"}) {
            assertThat(l.get(id).speedBand()).as(id).isEqualTo(SpeedBand.FROM_25_MPH);
        }
        for (String id : new String[] {"4456501", "4456502", "4616325", "4616341"}) {
            assertThat(l.get(id).status()).as(id).isEqualTo(TrafficLinkStatus.NO_DATA);
            assertThat(l.get(id).speedMph()).isNull();
            assertThat(l.get(id).travelTimeS()).isNull();
        }
        for (TrafficLink link : s.links()) {
            GeometrySource expected = switch (link.linkId()) {
                case "4456494", "4456501", "4616271" -> GeometrySource.LINK_POINTS;
                default -> GeometrySource.ENCODED_POLYLINE;
            };
            assertThat(link.geometrySource()).as(link.linkId()).isEqualTo(expected);
            for (double[] c : link.coordinates()) {
                assertThat(c[0]).isBetween(-74.30, -73.65);
                assertThat(c[1]).isBetween(40.45, 40.95);
            }
        }
        assertThat(l.get("4616271").geometryTruncated()).isTrue();
        assertThat(l.get("4616271").coordinates()).hasSize(12);
        assertThat(l.get("4456494").coordinates()).hasSize(11);
        assertThat(l.get("4456501").coordinates()).hasSize(10);
        assertThat(l.get("4616342").dataAsOf()).isEqualTo(Instant.parse("2026-11-01T20:03:10Z"));
        assertThat(l.get("4616333").coordinates()).hasSize(49);
    }

    private ObjectNode rec(ArrayNode records, String id, String asOf, String speed, String travel, String poly,
            String points) {
        ObjectNode r = records.addObject();
        r.put("link_id", id);
        r.put("data_as_of", asOf);
        r.put("speed", speed);
        r.put("travel_time", travel);
        r.put("encoded_poly_line", poly);
        if (points != null) {
            r.put("link_points", points);
        }
        r.put("link_name", "n" + id);
        return r;
    }

    private String pairs(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "" : " ").append("40.7").append(100 + i).append(",-73.99").append(900 + i);
        }
        return sb.toString();
    }

    @Test
    void classificationRules() throws Exception {
        ObjectNode root = M.createObjectNode();
        root.put("feedLatestDataAsOf", "2026-07-01T12:00:00.000");
        root.put("timeZone", "America/New_York");
        ArrayNode r = root.putArray("records");
        rec(r, "a-obs", "2026-07-01T11:50:00.000", "30", "100", POLY, null);
        rec(r, "b-stale", "2026-07-01T11:40:00.000", "30", "100", POLY, null);
        rec(r, "c-edge", "2026-07-01T11:45:00.000", "30", "100", POLY, null);
        rec(r, "d-zero", "2026-07-01T11:50:00.000", "0", "100", POLY, null);
        rec(r, "e-neg", "2026-07-01T11:50:00.000", "30", "-1", POLY, null);
        rec(r, "f-abc", "2026-07-01T11:50:00.000", "abc", "100", POLY, null);
        rec(r, "g-fast", "2026-07-01T11:50:00.000", "150", "100", POLY, null);
        rec(r, "", "2026-07-01T11:50:00.000", "30", "100", POLY, null);
        rec(r, "h-time", "yesterday", "30", "100", POLY, null);
        rec(r, "i-bad", "2026-07-01T11:50:00.000", "30", "100", "!!", "1,2 3,4");
        String points = pairs(16);
        assertThat(points.length()).isGreaterThanOrEqualTo(255);
        rec(r, "j-points", "2026-07-01T11:50:00.000", "30", "100", "!!", points);
        rec(r, "k-1", "2026-07-01T11:50:00.000", "9.99", "100", POLY, null);
        rec(r, "k-2", "2026-07-01T11:50:00.000", "10", "100", POLY, null);
        rec(r, "k-3", "2026-07-01T11:50:00.000", "24.99", "100", POLY, null);
        rec(r, "k-4", "2026-07-01T11:50:00.000", "25", "100", POLY, null);
        TrafficSnapshot s = parser.parse(M.writeValueAsBytes(root));
        assertThat(s.observedAt()).isEqualTo(Instant.parse("2026-07-01T16:00:00Z"));
        Map<String, TrafficLink> l = byId(s);
        assertThat(l.get("a-obs").status()).isEqualTo(TrafficLinkStatus.OBSERVED);
        assertThat(l.get("a-obs").speedBand()).isEqualTo(SpeedBand.FROM_25_MPH);
        assertThat(l.get("b-stale").status()).isEqualTo(TrafficLinkStatus.STALE);
        assertThat(l.get("b-stale").speedBand()).isNull();
        assertThat(l.get("c-edge").status()).isEqualTo(TrafficLinkStatus.OBSERVED);
        assertThat(l.get("d-zero").status()).isEqualTo(TrafficLinkStatus.NO_DATA);
        assertThat(l.get("e-neg").status()).isEqualTo(TrafficLinkStatus.NO_DATA);
        assertThat(l.get("f-abc").status()).isEqualTo(TrafficLinkStatus.NO_DATA);
        assertThat(l).doesNotContainKeys("g-fast", "", "h-time", "i-bad");
        assertThat(l.get("j-points").geometrySource()).isEqualTo(GeometrySource.LINK_POINTS);
        assertThat(l.get("j-points").geometryTruncated()).isTrue();
        assertThat(l.get("j-points").coordinates()).hasSize(15);
        assertThat(l.get("k-1").speedBand()).isEqualTo(SpeedBand.BELOW_10_MPH);
        assertThat(l.get("k-2").speedBand()).isEqualTo(SpeedBand.FROM_10_TO_25_MPH);
        assertThat(l.get("k-3").speedBand()).isEqualTo(SpeedBand.FROM_10_TO_25_MPH);
        assertThat(l.get("k-4").speedBand()).isEqualTo(SpeedBand.FROM_25_MPH);
        TrafficCounts c = s.counts();
        assertThat(c.total()).isEqualTo(15);
        assertThat(c.invalid()).isEqualTo(4);
        assertThat(c.observed() + c.stale() + c.noData() + c.invalid()).isEqualTo(c.total());
        assertThat(s.links()).hasSize(c.total() - c.invalid());
    }

    @Test
    void missingFeedTimeUsesTheNewestRecord() throws Exception {
        ObjectNode root = M.createObjectNode();
        ArrayNode r = root.putArray("records");
        rec(r, "x", "2026-07-01T11:50:00.000", "30", "100", POLY, null);
        rec(r, "y", "2026-07-01T11:55:00.000", "30", "100", POLY, null);
        rec(r, "z", "bad", "30", "100", POLY, null);
        TrafficSnapshot s = parser.parse(M.writeValueAsBytes(root));
        assertThat(s.observedAt()).isEqualTo(Instant.parse("2026-07-01T15:55:00Z"));
    }

    @Test
    void fileLevelFailures() {
        assertThatThrownBy(() -> parser.parse("not json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not valid JSON");
        assertThatThrownBy(() -> parser.parse("{\"records\":[]}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no records");
    }
}
