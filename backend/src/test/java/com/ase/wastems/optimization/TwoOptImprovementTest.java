package com.ase.wastems.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TwoOptImprovementTest {

    static ArrayCostMatrix m(double[][] d) {
        return new ArrayCostMatrix(d, d);
    }

    static final double[][] A = {{0, 1, 10, 5}, {2, 0, 1, 20}, {1, 20, 0, 10}, {10, 20, 2, 0}};
    static final double[][] B = {{0, 1, 10, 5, 10}, {20, 0, 1, 5, 5}, {2, 20, 0, 5, 1}, {1, 20, 1, 0, 10},
            {1, 5, 10, 1, 0}};
    static final double[][] C = {{0, 5, 2, 3, 1}, {2, 0, 5, 1, 5}, {8, 8, 0, 3, 2}, {5, 1, 2, 0, 5},
            {3, 2, 3, 3, 0}};

    private final TwoOptImprovement twoOpt = new TwoOptImprovement();

    @Test
    void symmetricShortcutWouldAcceptAWorseningMove() {
        ArrayCostMatrix a = m(A);
        TwoOptImprovement.Result r = twoOpt.improve(a, List.of(1, 2, 3));
        assertThat(r.order()).containsExactly(1, 2, 3);
        assertThat(r.acceptedMoves()).isZero();
        assertThat(TourCost.distance(a, List.of(1, 2, 3))).isCloseTo(22, within(1e-9));
        // Symmetric delta of reversing positions 1..3 looks improving ...
        double symmetric = A[0][3] + A[1][0] - A[0][1] - A[3][0];
        assertThat(symmetric).isCloseTo(-4, within(1e-9));
        // ... but the exact directed cost of the reversed tour is worse.
        assertThat(TourCost.distance(a, List.of(3, 2, 1))).isCloseTo(29, within(1e-9));
    }

    /** Best move on route [0,o1..ok,0] under the symmetric delta: returns {i,k} (1-based positions). */
    private static int[] bestSymmetricMove(double[][] d, int[] r) {
        double best = -1e-9;
        int[] mv = null;
        for (int i = 1; i < r.length - 2; i++) {
            for (int k = i + 1; k < r.length - 1; k++) {
                double delta = d[r[i - 1]][r[k]] + d[r[i]][r[k + 1]] - d[r[i - 1]][r[i]] - d[r[k]][r[k + 1]];
                if (delta < best) {
                    best = delta;
                    mv = new int[] {i, k};
                }
            }
        }
        return mv;
    }

    @Test
    void symmetricShortcutWouldPickADifferentMove() {
        ArrayCostMatrix b = m(B);
        TwoOptImprovement.Result r = twoOpt.improve(b, List.of(1, 2, 3, 4));
        assertThat(r.order()).containsExactly(1, 2, 4, 3);
        assertThat(r.acceptedMoves()).isEqualTo(1);
        assertThat(TourCost.distance(b, r.order())).isCloseTo(5, within(1e-9));
        assertThat(TourCost.distance(b, List.of(1, 2, 3, 4))).isCloseTo(18, within(1e-9));

        int[] route = {0, 1, 2, 3, 4, 0};
        int[] first = bestSymmetricMove(B, route);
        assertThat(first).containsExactly(2, 3);
        assertThat(first).isNotEqualTo(new int[] {3, 4});
        // iterate the symmetric rule
        int[] cur = route.clone();
        for (int guard = 0; guard < 20; guard++) {
            int[] mv = bestSymmetricMove(B, cur);
            if (mv == null) {
                break;
            }
            for (int lo = mv[0], hi = mv[1]; lo < hi; lo++, hi--) {
                int t = cur[lo];
                cur[lo] = cur[hi];
                cur[hi] = t;
            }
        }
        assertThat(cur).containsExactly(0, 1, 3, 2, 4, 0);
        assertThat(TourCost.distance(b, List.of(1, 3, 2, 4))).isCloseTo(9, within(1e-9));
    }

    @Test
    void improvesNearestNeighborOrder() {
        ArrayCostMatrix c = m(C);
        TwoOptImprovement.Result r = twoOpt.improve(c, List.of(4, 1, 3, 2));
        assertThat(r.order()).containsExactly(4, 2, 3, 1);
        assertThat(r.acceptedMoves()).isEqualTo(1);
        assertThat(TourCost.distance(c, r.order())).isCloseTo(10, within(1e-9));
        assertThat(TourCost.distance(c, List.of(4, 1, 3, 2))).isCloseTo(14, within(1e-9));
    }

    @Test
    void resultIsTwoOptimalAndNeverWorse() {
        int n = 12;
        for (long seed = 1; seed <= 20; seed++) {
            Random rnd = new Random(seed);
            double[][] d = new double[n + 1][n + 1];
            for (int i = 0; i <= n; i++) {
                for (int j = 0; j <= n; j++) {
                    d[i][j] = i == j ? 0 : 1 + rnd.nextInt(1000);
                }
            }
            ArrayCostMatrix mat = m(d);
            PlanningScenario s = new PlanningScenario(mat, new double[n], 1000, 0);
            List<Integer> start = new NearestNeighborConstruction().construct(s);
            List<Integer> result = twoOpt.improve(mat, start).order();
            assertThat(result).hasSize(n).doesNotHaveDuplicates();
            assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            double dist = TourCost.distance(mat, result);
            assertThat(dist).isLessThanOrEqualTo(TourCost.distance(mat, start) + 1e-9);
            for (int i = 0; i < n; i++) {
                for (int k = i + 1; k < n; k++) {
                    List<Integer> reversed = new ArrayList<>(result.subList(0, i));
                    List<Integer> seg = new ArrayList<>(result.subList(i, k + 1));
                    java.util.Collections.reverse(seg);
                    reversed.addAll(seg);
                    reversed.addAll(result.subList(k + 1, n));
                    assertThat(TourCost.distance(mat, reversed)).isGreaterThanOrEqualTo(dist - 1e-6);
                }
            }
        }
    }

    @Test
    void shortOrdersAreReturnedUnchanged() {
        ArrayCostMatrix c = m(C);
        assertThat(twoOpt.improve(c, List.of()).order()).isEmpty();
        assertThat(twoOpt.improve(c, List.of()).acceptedMoves()).isZero();
        TwoOptImprovement.Result one = twoOpt.improve(c, List.of(1));
        assertThat(one.order()).containsExactly(1);
        assertThat(one.acceptedMoves()).isZero();
    }
}
