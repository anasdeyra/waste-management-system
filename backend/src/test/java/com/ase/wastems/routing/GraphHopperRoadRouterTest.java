package com.ase.wastems.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class GraphHopperRoadRouterTest {

    static final String SHA = "e1f2fabaa937117f3a4c25582fb1ac4026fbb6fe8f3b9882f705e88a341d6a01";
    static final WgsPoint CENTRE_WORTH = new WgsPoint(-74.00212207, 40.71528035);
    static final WgsPoint BROADWAY_LEONARD = new WgsPoint(-74.0040862, 40.71705309);
    static final WgsPoint QUEENS = new WgsPoint(-73.80, 40.70);

    static RoutingProperties props(String location, String sha, String cacheDir) {
        return new RoutingProperties(location, sha, "osm-mn01-", cacheDir, 100);
    }

    static GraphHopperRoadRouter shared;

    @BeforeAll
    static void setUp() {
        shared = new GraphHopperRoadRouter(
                props("classpath:routing/mn01-roads.osm", SHA, "target/routing-cache-unit"),
                new DefaultResourceLoader());
    }

    @AfterAll
    static void tearDown() {
        shared.destroy();
    }

    static double haversine(WgsPoint a, WgsPoint b) {
        double r = 6371008.8;
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude()))
                        * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    @Test
    void versionIdComesFromTheChecksum() {
        assertThat(shared.roadGraphVersion()).isEqualTo("osm-mn01-e1f2fabaa937");
        assertThat(shared.routingEngine()).startsWith("GraphHopper 11.1");
    }

    @Test
    void directedLegsDifferByDirection() {
        RoadLeg ab = shared.route(CENTRE_WORTH, BROADWAY_LEONARD);
        RoadLeg ba = shared.route(BROADWAY_LEONARD, CENTRE_WORTH);
        assertThat(ab.distanceM()).isBetween(250.0, 400.0);
        assertThat(ba.distanceM()).isBetween(650.0, 950.0);
        assertThat(ba.distanceM() - ab.distanceM()).isGreaterThan(300);
        assertThat(ab.travelTimeS()).isPositive();
        assertThat(ba.travelTimeS()).isPositive();
        assertThat(ab.geometry().size()).isGreaterThanOrEqualTo(2);
        assertThat(haversine(ab.geometry().get(0), CENTRE_WORTH)).isLessThan(50);
        assertThat(haversine(ab.geometry().get(ab.geometry().size() - 1), BROADWAY_LEONARD)).isLessThan(50);
    }

    @Test
    void basketsSnapClose() {
        assertThat(shared.snapDistanceMeters(CENTRE_WORTH)).isLessThan(30);
    }

    @Test
    void farPointIsNotRoutable() {
        assertThatThrownBy(() -> shared.snapDistanceMeters(QUEENS))
                .isInstanceOfSatisfying(RoadLegUnavailableException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE);
                    assertThat(e.getMessage()).contains("limit 100 m");
                });
    }

    @Test
    void checksumMismatchIsExplicit(@TempDir Path cache) {
        GraphHopperRoadRouter r = new GraphHopperRoadRouter(
                props("classpath:routing/mn01-roads.osm", "0".repeat(64), cache.toString()),
                new DefaultResourceLoader());
        assertThatThrownBy(r::roadGraphVersion).isInstanceOf(RoutingUnavailableException.class)
                .hasMessageContaining("checksum mismatch").hasMessageContaining(SHA);
    }

    @Test
    void missingSnapshotIsExplicit(@TempDir Path cache) {
        GraphHopperRoadRouter r = new GraphHopperRoadRouter(
                props("classpath:routing/does-not-exist.osm", SHA, cache.toString()),
                new DefaultResourceLoader());
        assertThatThrownBy(r::roadGraphVersion).isInstanceOf(RoutingUnavailableException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void corruptSnapshotIsExplicit(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.osm");
        byte[] content = "<osm version=\"0.6\"><node id=\"1\"".getBytes(StandardCharsets.UTF_8);
        Files.write(bad, content);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        Path cache = Files.createDirectory(dir.resolve("cache"));
        GraphHopperRoadRouter r = new GraphHopperRoadRouter(props("file:" + bad, sha, cache.toString()),
                new DefaultResourceLoader());
        assertThatThrownBy(r::roadGraphVersion).isInstanceOf(RoutingUnavailableException.class)
                .hasMessageContaining("import failed");
    }
}
