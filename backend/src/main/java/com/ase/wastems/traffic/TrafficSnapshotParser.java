package com.ase.wastems.traffic;

import com.ase.wastems.config.ServiceAreaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Parses and classifies the committed NYC DOT snapshot (docs/geospatial-data.md section 3.2). The
 * first matching rule wins: blank link id, unparsable time, unusable geometry, missing reading,
 * implausible speed (invalid); older than the staleness window (stale); otherwise observed.
 */
public final class TrafficSnapshotParser {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ServiceAreaProperties area;
    private final int staleAfterMinutes;
    private final double maxPlausibleSpeedMph;
    private final ObjectMapper mapper = new ObjectMapper();

    public TrafficSnapshotParser(ServiceAreaProperties area, int staleAfterMinutes, double maxPlausibleSpeedMph) {
        this.area = area;
        this.staleAfterMinutes = staleAfterMinutes;
        this.maxPlausibleSpeedMph = maxPlausibleSpeedMph;
    }

    public TrafficSnapshot parse(byte[] bytes) {
        JsonNode root;
        try {
            root = mapper.readTree(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("not valid JSON", e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("not valid JSON");
        }
        JsonNode records = root.get("records");
        if (records == null || !records.isArray() || records.isEmpty()) {
            throw new IllegalArgumentException("no records");
        }
        ZoneId zone = ZoneId.of(text(root, "timeZone") == null ? "America/New_York" : text(root, "timeZone"));

        Instant observedAt = null;
        String feed = text(root, "feedLatestDataAsOf");
        if (feed != null) {
            ZonedDateTime z = zoned(feed, zone);
            observedAt = z == null ? null : z.toInstant();
        }
        if (observedAt == null) {
            for (JsonNode r : records) {
                ZonedDateTime z = zoned(text(r, "data_as_of"), zone);
                if (z != null && (observedAt == null || z.toInstant().isAfter(observedAt))) {
                    observedAt = z.toInstant();
                }
            }
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("no records");
        }
        Instant staleBefore = observedAt.minusSeconds(staleAfterMinutes * 60L);

        List<TrafficLink> links = new ArrayList<>();
        int observed = 0;
        int stale = 0;
        int noData = 0;
        int invalid = 0;
        for (JsonNode r : records) {
            TrafficLink link = classify(r, zone, staleBefore);
            if (link == null) {
                invalid++;
                continue;
            }
            links.add(link);
            switch (link.status()) {
                case OBSERVED -> observed++;
                case STALE -> stale++;
                case NO_DATA -> noData++;
            }
        }
        Instant retrievedAt = null;
        String retrieved = text(root, "retrievedAt");
        if (retrieved != null) {
            try {
                retrievedAt = Instant.parse(retrieved);
            } catch (RuntimeException e) {
                retrievedAt = null;
            }
        }
        String id = "nyc-dot-i4gi-tjb9:" + records.size() + ":" + sha12(bytes);
        return new TrafficSnapshot(id, text(root, "source"), text(root, "sourceUrl"), retrievedAt, observedAt,
                LOCAL.format(observedAt.atZone(zone)), zone.getId(),
                new TrafficCounts(records.size(), observed, stale, noData, invalid), List.copyOf(links));
    }

    private TrafficLink classify(JsonNode r, ZoneId zone, Instant staleBefore) {
        String linkId = text(r, "link_id");
        if (linkId == null || linkId.isBlank()) {
            return null;
        }
        ZonedDateTime when = zoned(text(r, "data_as_of"), zone);
        if (when == null) {
            return null;
        }
        List<double[]> coordinates = null;
        GeometrySource source = null;
        boolean truncated = false;
        String encoded = text(r, "encoded_poly_line");
        if (encoded != null) {
            try {
                List<double[]> pts = PolylineDecoder.decode(encoded);
                if (pts.size() >= 2 && pts.stream().allMatch(p -> area.contains(p[1], p[0]))) {
                    coordinates = new ArrayList<>();
                    for (double[] p : pts) {
                        coordinates.add(new double[] {p[1], p[0]});
                    }
                    source = GeometrySource.ENCODED_POLYLINE;
                }
            } catch (IllegalArgumentException e) {
                coordinates = null;
            }
        }
        if (coordinates == null) {
            String raw = text(r, "link_points");
            if (raw != null) {
                List<String> tokens = new ArrayList<>(List.of(raw.trim().split("\\s+")));
                if (raw.length() >= 255 && !tokens.isEmpty()) {
                    tokens.remove(tokens.size() - 1);
                    truncated = true;
                }
                List<double[]> pts = new ArrayList<>();
                for (String t : tokens) {
                    double[] p = pair(t);
                    if (p != null) {
                        pts.add(new double[] {p[1], p[0]});
                    }
                }
                if (pts.size() >= 2) {
                    coordinates = pts;
                    source = GeometrySource.LINK_POINTS;
                } else {
                    truncated = false;
                }
            }
        }
        if (coordinates == null) {
            return null;
        }
        Double speed = number(text(r, "speed"));
        Double travel = number(text(r, "travel_time"));
        String local = LOCAL.format(when.toLocalDateTime());
        String name = text(r, "link_name");
        if (speed == null || travel == null || speed <= 0 || travel <= 0) {
            return new TrafficLink(linkId, name, text(r, "borough"), text(r, "owner"), TrafficLinkStatus.NO_DATA, null,
                    null, null, when.toInstant(), local, source, truncated, coordinates);
        }
        if (speed > maxPlausibleSpeedMph) {
            return null;
        }
        int travelS = (int) Math.round(travel);
        if (when.toInstant().isBefore(staleBefore)) {
            return new TrafficLink(linkId, name, text(r, "borough"), text(r, "owner"), TrafficLinkStatus.STALE, speed,
                    travelS, null, when.toInstant(), local, source, truncated, coordinates);
        }
        SpeedBand band = speed < 10 ? SpeedBand.BELOW_10_MPH
                : speed < 25 ? SpeedBand.FROM_10_TO_25_MPH : SpeedBand.FROM_25_MPH;
        return new TrafficLink(linkId, name, text(r, "borough"), text(r, "owner"), TrafficLinkStatus.OBSERVED, speed,
                travelS, band, when.toInstant(), local, source, truncated, coordinates);
    }

    /** {latitude, longitude} of a "lat,lon" token inside the service area, else null. */
    private double[] pair(String token) {
        String[] parts = token.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        Double lat = number(parts[0]);
        Double lon = number(parts[1]);
        if (lat == null || lon == null || !area.contains(lon, lat)) {
            return null;
        }
        return new double[] {lat, lon};
    }

    private static Double number(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        try {
            double v = Double.parseDouble(t.trim());
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ZonedDateTime zoned(String text, ZoneId zone) {
        if (text == null) {
            return null;
        }
        try {
            return ZonedDateTime.of(LocalDateTime.parse(text.trim()), zone);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String sha12(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
