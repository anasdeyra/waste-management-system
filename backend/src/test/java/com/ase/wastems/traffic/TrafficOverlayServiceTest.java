package com.ase.wastems.traffic;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.config.ServiceAreaProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class TrafficOverlayServiceTest {

    static final ServiceAreaProperties AREA = new ServiceAreaProperties(-74.30, -73.65, 40.45, 40.95);

    private TrafficOverlayService service(String location, String instant) {
        return new TrafficOverlayService(new TrafficProperties(location, 15, 100), AREA, new DefaultResourceLoader(),
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void committedSnapshotHasAnAge() {
        TrafficOverlay o = service("classpath:traffic/mn01-traffic-links.json", "2026-11-01T22:08:05Z").overlay();
        assertThat(o.available()).isTrue();
        assertThat(o.ageMinutes()).isEqualTo(120);
        assertThat(o.snapshot().links()).hasSize(12);
    }

    @Test
    void missingSnapshotIsUnavailable() {
        TrafficOverlay o = service("classpath:traffic/does-not-exist.json", "2026-11-01T22:08:05Z").overlay();
        assertThat(o.available()).isFalse();
        assertThat(o.unavailableReason()).startsWith("Traffic snapshot not found");
        assertThat(o.snapshot()).isNull();
    }

    @Test
    void invalidSnapshotIsUnavailable(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.json");
        Files.writeString(bad, "{");
        TrafficOverlay o = service("file:" + bad, "2026-11-01T22:08:05Z").overlay();
        assertThat(o.available()).isFalse();
        assertThat(o.unavailableReason()).startsWith("Traffic snapshot is not valid JSON");
    }
}
