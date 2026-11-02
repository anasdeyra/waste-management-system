package com.ase.wastems.traffic;

import java.util.ArrayList;
import java.util.List;

/** Decoder of Google encoded polylines (precision 1e5) as published by the NYC DOT feed. */
public final class PolylineDecoder {

    private PolylineDecoder() {}

    /**
     * Returns {latitude, longitude} pairs. The feed doubles every backslash, so each pair of
     * backslashes is first reduced to one. Throws IllegalArgumentException on a character outside
     * '?'..'~' or a truncated value.
     */
    public static List<double[]> decode(String encoded) {
        String s = encoded.replace("\\\\", "\\");
        List<double[]> out = new ArrayList<>();
        int i = 0;
        long lat = 0;
        long lon = 0;
        while (i < s.length()) {
            long[] delta = new long[2];
            for (int k = 0; k < 2; k++) {
                int shift = 0;
                long result = 0;
                int b;
                do {
                    if (i >= s.length()) {
                        throw new IllegalArgumentException("truncated polyline");
                    }
                    char c = s.charAt(i++);
                    if (c < 63 || c > 126) {
                        throw new IllegalArgumentException("invalid polyline character at " + (i - 1));
                    }
                    b = c - 63;
                    result |= (long) (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                delta[k] = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            }
            lat += delta[0];
            lon += delta[1];
            out.add(new double[] {lat / 1e5, lon / 1e5});
        }
        return out;
    }
}
