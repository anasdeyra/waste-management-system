package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.List;

/**
 * Best-improvement 2-opt on the exact directed tour distance. Reversing a segment of a
 * directed tour changes the cost of every leg inside the segment, so each candidate move is
 * evaluated with the reversed internal legs re-costed from the matrix. This is deliberately
 * not the symmetric edge delta {@code d(a,c)+d(b,d)-d(a,b)-d(c,d)}, which is wrong when
 * {@code d(x,y) != d(y,x)}. Prefix sums of the forward and backward leg costs make each
 * candidate O(1). Ties keep the first best move (lowest i, then lowest k).
 */
public final class TwoOptImprovement {

    public record Result(List<Integer> order, int acceptedMoves) {}

    public static final double EPSILON_M = 1e-6;
    public static final String CONFIGURATION = "2-opt best improvement on the exact directed tour distance "
            + "(every reversed segment re-costed, no symmetric edge delta); epsilon 1e-6 m";

    private static final int MAX_MOVES = 1_000_000;

    public Result improve(DirectedCostMatrix m, List<Integer> start) {
        int kn = start.size();
        if (kn < 2) {
            return new Result(List.copyOf(start), 0);
        }
        int[] r = new int[kn + 2];
        r[0] = 0;
        r[kn + 1] = 0;
        for (int i = 0; i < kn; i++) {
            r[i + 1] = start.get(i);
        }
        int moves = 0;
        double[] f = new double[kn + 2];
        double[] b = new double[kn + 2];
        while (true) {
            f[0] = 0;
            b[0] = 0;
            for (int t = 1; t <= kn + 1; t++) {
                f[t] = f[t - 1] + m.distance(r[t - 1], r[t]);
                b[t] = b[t - 1] + m.distance(r[t], r[t - 1]);
            }
            double bestDelta = -EPSILON_M;
            int bi = -1;
            int bk = -1;
            for (int i = 1; i <= kn - 1; i++) {
                for (int k = i + 1; k <= kn; k++) {
                    double delta = m.distance(r[i - 1], r[k]) + m.distance(r[i], r[k + 1]) + (b[k] - b[i])
                            - m.distance(r[i - 1], r[i]) - m.distance(r[k], r[k + 1]) - (f[k] - f[i]);
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bi = i;
                        bk = k;
                    }
                }
            }
            if (bi == -1) {
                break;
            }
            for (int lo = bi, hi = bk; lo < hi; lo++, hi--) {
                int tmp = r[lo];
                r[lo] = r[hi];
                r[hi] = tmp;
            }
            moves++;
            if (moves > MAX_MOVES) {
                throw new IllegalStateException("2-opt did not converge");
            }
        }
        List<Integer> out = new ArrayList<>(kn);
        for (int i = 1; i <= kn; i++) {
            out.add(r[i]);
        }
        return new Result(List.copyOf(out), moves);
    }
}
