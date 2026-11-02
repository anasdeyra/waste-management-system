package com.ase.wastems.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SweepBaselineTest {

    private static final double[] LON4 = {0, 1, 0, -1, 0};
    private static final double[] LAT4 = {0, 0, 1, 0, -1};

    private static ArrayCostMatrix m(double[][] d) {
        return new ArrayCostMatrix(d, d);
    }

    private static double[][] symmetric(int size, double v) {
        double[][] d = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                d[i][j] = i == j ? 0 : v;
            }
        }
        return d;
    }

    private static PlanningScenario scenario(double[][] d, double[] lon, double[] lat) {
        double[] demand = new double[d.length - 1];
        java.util.Arrays.fill(demand, 1);
        return new PlanningScenario(m(d), demand, 100, 0, lon, lat);
    }

    // (a) four compass points, all candidates tie: counter-clockwise from the smallest angle
    @Test
    void allCandidatesTieKeepsCounterClockwiseFromSmallestAngle() {
        SweepBaseline.Result r = new SweepBaseline().order(scenario(symmetric(5, 5), LON4, LAT4));
        assertThat(r.order()).containsExactly(1, 2, 3, 4);
        assertThat(r.clockwise()).isFalse();
        assertThat(r.startNode()).isEqualTo(1);
    }

    // (b) angle values on the equirectangular projection
    @Test
    void polarAnglesUseTheLocalProjectionAndStayBelowTwoPi() {
        double[] lon = {0, 1, 0, 0, 1};
        double[] lat = {60, 60.5, 59, 60, 60};
        PlanningScenario s = scenario(symmetric(5, 1), lon, lat);
        // x = 1 * cos(60 deg) = 0.5, y = 0.5
        assertThat(SweepBaseline.polarAngle(s, 1)).isCloseTo(Math.PI / 4, within(1e-12));
        assertThat(SweepBaseline.polarAngle(s, 2)).isCloseTo(3 * Math.PI / 2, within(1e-12));
        assertThat(SweepBaseline.polarAngle(s, 4)).isEqualTo(0.0);
        assertThat(SweepBaseline.planarRadius(s, 1)).isCloseTo(Math.sqrt(0.5), within(1e-12));
    }

    // (c) equal angle: nearer first; identical coordinates: lower index first
    @Test
    void ties_nearerFirstThenLowerIndex() {
        // nodes 1 and 2 on the same ray (2 is nearer), nodes 3 and 4 identical coordinates
        double[] lon = {0, 2, 1, 0, 0};
        double[] lat = {0, 0, 0, 1, 1};
        double[][] d = symmetric(5, 5);
        SweepBaseline.Result r = new SweepBaseline().order(scenario(d, lon, lat));
        // sorted: east ray (2 nearer, then 1), north ray (3, 4); all ties -> CCW from k = 0
        assertThat(r.order()).containsExactly(2, 1, 3, 4);
    }

    // (d) direction choice on a directed matrix
    @Test
    void choosesClockwiseWhenCounterClockwiseLegsAreExpensive() {
        double[][] d = symmetric(5, 1);
        // counter-clockwise legs 1->2, 2->3, 3->4, 4->1 cost 10; clockwise (reverse) cost 1
        d[1][2] = 10;
        d[2][3] = 10;
        d[3][4] = 10;
        d[4][1] = 10;
        PlanningScenario s = scenario(d, LON4, LAT4);
        SweepBaseline.Result r = new SweepBaseline().order(s);
        assertThat(r.clockwise()).isTrue();
        // all four clockwise rays cost 1 (depot) + 1 + 1 + 1 + 1 (depot) = 5; the first, start position 0 -> stop 1
        assertThat(r.order()).containsExactly(1, 4, 3, 2);
        assertThat(TourCost.distance(s.matrix(), r.order())).isEqualTo(5.0);
    }

    // (e) start-ray choice: only one start position is cheapest
    @Test
    void choosesTheCheapestStartPosition() {
        double[][] d = symmetric(5, 100);
        // only the counter-clockwise cycle starting at stop 3 is cheap: 0->3 (1), 3->4, 4->1, 1->2 (1 each), 2->0 (1)
        d[0][3] = 1;
        d[3][4] = 1;
        d[4][1] = 1;
        d[1][2] = 1;
        d[2][0] = 1;
        PlanningScenario s = scenario(d, LON4, LAT4);
        SweepBaseline.Result r = new SweepBaseline().order(s);
        assertThat(r.order()).containsExactly(3, 4, 1, 2);
        assertThat(r.startNode()).isEqualTo(3);
        assertThat(r.clockwise()).isFalse();
        assertThat(TourCost.distance(s.matrix(), r.order())).isEqualTo(5.0);
    }

    // (f) brute-force agreement with an independent construction of the 2n candidates
    @Test
    void agreesWithBruteForceOverTheTwoNCandidates() {
        Random rnd = new Random(20261102);
        int instances = 0;
        for (int n = 1; n <= 12; n++) {
            for (int rep = 0; rep < 5; rep++) {
                instances++;
                double[][] d = new double[n + 1][n + 1];
                double[] lon = new double[n + 1];
                double[] lat = new double[n + 1];
                for (int i = 0; i <= n; i++) {
                    lon[i] = -74.02 + rnd.nextDouble() * 0.05;
                    lat[i] = 40.70 + rnd.nextDouble() * 0.05;
                    for (int j = 0; j <= n; j++) {
                        d[i][j] = i == j ? 0 : 1 + rnd.nextInt(500);
                    }
                }
                PlanningScenario s = scenario(d, lon, lat);
                // independent sort by atan2 on the projected plane
                List<Integer> sorted = new ArrayList<>();
                for (int i = 1; i <= n; i++) {
                    sorted.add(i);
                }
                double c = Math.cos(Math.toRadians(lat[0]));
                sorted.sort((a, b) -> {
                    double ta = angle(lon[a] - lon[0], lat[a] - lat[0], c);
                    double tb = angle(lon[b] - lon[0], lat[b] - lat[0], c);
                    if (ta != tb) {
                        return Double.compare(ta, tb);
                    }
                    double ra = Math.hypot((lon[a] - lon[0]) * c, lat[a] - lat[0]);
                    double rb = Math.hypot((lon[b] - lon[0]) * c, lat[b] - lat[0]);
                    return ra != rb ? Double.compare(ra, rb) : Integer.compare(a, b);
                });
                double min = Double.POSITIVE_INFINITY;
                List<Integer> first = null;
                for (int dir = 0; dir < 2; dir++) {
                    for (int k = 0; k < n; k++) {
                        List<Integer> cand = new ArrayList<>();
                        for (int j = 0; j < n; j++) {
                            cand.add(sorted.get(dir == 0 ? (k + j) % n : ((k - j) % n + n) % n));
                        }
                        double cost = TourCost.distance(s.matrix(), cand);
                        if (cost < min) {
                            min = cost;
                            first = cand;
                        }
                    }
                }
                SweepBaseline.Result r = new SweepBaseline().order(s);
                assertThat(TourCost.distance(s.matrix(), r.order())).isEqualTo(min);
                assertThat(r.order()).isEqualTo(first);
                assertThat(r.order()).hasSize(n).doesNotHaveDuplicates();
            }
        }
        assertThat(instances).isGreaterThanOrEqualTo(50);
    }

    private static double angle(double dx, double dy, double cos) {
        double t = Math.atan2(dy, dx * cos);
        if (t < 0) {
            t += 2 * Math.PI;
        }
        return t >= 2 * Math.PI ? 0 : t;
    }

    // (g) single stop and missing coordinates
    @Test
    void singleStopAndMissingCoordinates() {
        double[][] d = {{0, 3}, {4, 0}};
        SweepBaseline.Result r = new SweepBaseline().order(scenario(d, new double[] {0, 1}, new double[] {0, 1}));
        assertThat(r.order()).containsExactly(1);
        assertThat(r.clockwise()).isFalse();
        PlanningScenario none = new PlanningScenario(m(d), new double[] {1}, 10, 0);
        assertThatThrownBy(() -> new SweepBaseline().order(none)).isInstanceOf(InfeasibleScenarioException.class);
    }
}
