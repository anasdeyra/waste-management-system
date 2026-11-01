package com.ase.wastems.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class RoadNetworkMatrixProviderTest {

    private static final class FakeRouter implements RoadRouter {
        final boolean snapFails;
        int routeCalls;

        FakeRouter(boolean snapFails) {
            this.snapFails = snapFails;
        }

        @Override
        public String roadGraphVersion() {
            return "fake-v1";
        }

        @Override
        public String routingEngine() {
            return "fake";
        }

        @Override
        public double snapDistanceMeters(WgsPoint p) {
            if (snapFails && p.longitude() > 0) {
                throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE,
                        "too far");
            }
            return 1;
        }

        @Override
        public RoadLeg route(WgsPoint a, WgsPoint b) {
            routeCalls++;
            if (a.longitude() == 2 && b.longitude() == 1) {
                throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.NO_ROAD_PATH,
                        "connection not found");
            }
            return new RoadLeg(1000 * Math.abs(a.longitude()) + Math.abs(b.longitude()) + 0.12345,
                    10 * Math.abs(a.longitude()) + Math.abs(b.longitude()), List.of(a, b));
        }
    }

    private static double round3(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }

    private static List<WgsPoint> nodes(double... lons) {
        return java.util.Arrays.stream(lons).mapToObj(l -> new WgsPoint(l, 40.7)).toList();
    }

    @Test
    void indicesFollowNodeOrder() {
        List<WgsPoint> nodes = nodes(-1, -2, -3);
        RoadMatrix m = new RoadNetworkMatrixProvider(new FakeRouter(true)).buildMatrix(nodes);
        assertThat(m.size()).isEqualTo(3);
        assertThat(m.roadGraphVersion()).isEqualTo("fake-v1");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == j) {
                    assertThat(m.distance(i, j)).isZero();
                    assertThat(m.travelTime(i, j)).isZero();
                    assertThat(m.leg(i, j).geometry()).containsExactly(nodes.get(i));
                } else {
                    double expected = round3(1000 * Math.abs(nodes.get(i).longitude())
                            + Math.abs(nodes.get(j).longitude()) + 0.12345);
                    assertThat(m.distance(i, j)).isEqualTo(expected);
                    assertThat(m.leg(i, j).geometry()).containsExactly(nodes.get(i), nodes.get(j));
                    assertThat(m.distance(i, j)).isEqualTo(Math.round(m.distance(i, j) * 1000) / 1000.0);
                }
            }
        }
        assertThat(m.distance(0, 1)).isEqualTo(1002.123);
        assertThat(m.distance(0, 1)).isNotEqualTo(m.distance(1, 0));
    }

    @Test
    void missingLegIsExplicit() {
        assertThatThrownBy(() -> new RoadNetworkMatrixProvider(new FakeRouter(false)).buildMatrix(nodes(0, 1, 2)))
                .isInstanceOfSatisfying(RoadLegUnavailableException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(RoadLegUnavailableException.Reason.NO_ROAD_PATH);
                    assertThat(e.getFromNode()).isEqualTo(2);
                    assertThat(e.getToNode()).isEqualTo(1);
                    assertThat(e.getMessage()).contains("connection not found");
                });
    }

    @Test
    void unroutablePointFailsBeforeAnyLegIsRouted() {
        FakeRouter fake = new FakeRouter(true);
        assertThatThrownBy(() -> new RoadNetworkMatrixProvider(fake).buildMatrix(nodes(-1, -2, 0.5)))
                .isInstanceOfSatisfying(RoadLegUnavailableException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE);
                    assertThat(e.getFromNode()).isEqualTo(2);
                    assertThat(e.getToNode()).isEqualTo(-1);
                });
        assertThat(fake.routeCalls).isZero();
    }

    @Test
    void tooFewNodesAreRejected() {
        assertThatThrownBy(() -> new RoadNetworkMatrixProvider(new FakeRouter(false)).buildMatrix(nodes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void realSnapshotMatrixIsDirected() {
        GraphHopperRoadRouter router = new GraphHopperRoadRouter(new RoutingProperties(
                "classpath:routing/mn01-roads.osm", GraphHopperRoadRouterTest.SHA, "osm-mn01-",
                "target/routing-cache-unit", 100), new DefaultResourceLoader());
        try {
            RoadMatrix m = new RoadNetworkMatrixProvider(router).buildMatrix(List.of(
                    new WgsPoint(-74.0104183829902, 40.7255225360961),
                    GraphHopperRoadRouterTest.CENTRE_WORTH, GraphHopperRoadRouterTest.BROADWAY_LEONARD));
            assertThat(m.size()).isEqualTo(3);
            assertThat(m.distance(1, 2)).isBetween(250.0, 400.0);
            assertThat(m.distance(2, 1)).isBetween(650.0, 950.0);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (i != j) {
                        assertThat(m.distance(i, j)).isPositive();
                    }
                }
            }
            assertThat(m.roadGraphVersion()).isEqualTo("osm-mn01-e1f2fabaa937");
        } finally {
            router.destroy();
        }
    }
}
