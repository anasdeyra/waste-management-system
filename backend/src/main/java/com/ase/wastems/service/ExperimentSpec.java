package com.ase.wastems.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Validated and normalized request of an experiment batch (profiles x seeds x sizes x repetitions). */
public record ExperimentSpec(List<DemandProfile> profiles, List<Long> seeds, List<Integer> sizes,
        int repetitions, String label) {

    public static final int MAX_RUNS = 180;
    public static final String CANONICAL_LABEL = "Fixed MN01 experiment";
    public static final String SUBSET_LABEL = "MN01 experiment subset";
    private static final List<DemandProfile> CANONICAL_PROFILES = List.of(DemandProfile.values());
    private static final List<Long> CANONICAL_SEEDS = List.of(1L, 2L, 3L, 4L, 5L);
    private static final List<Integer> CANONICAL_SIZES = List.of(10, 25, 50, 100);

    public static ExperimentSpec of(List<DemandProfile> profiles, List<Long> seeds, List<Integer> sizes,
            Integer repetitions, String label) {
        List<DemandProfile> p = profiles == null ? CANONICAL_PROFILES : profiles;
        List<Long> s = seeds == null ? CANONICAL_SEEDS : seeds;
        List<Integer> z = sizes == null ? CANONICAL_SIZES : sizes;
        int r = repetitions == null ? 1 : repetitions;
        if (p.isEmpty() || hasNull(p) || new HashSet<>(p).size() != p.size()) {
            throw new InvalidRequestException("profiles", "must list each demand profile at most once, at least one");
        }
        if (s.isEmpty() || s.size() > 10 || hasNull(s) || new HashSet<>(s).size() != s.size()
                || s.stream().anyMatch(v -> v < 1 || v > 1_000_000)) {
            throw new InvalidRequestException("seeds", "must be 1 to 10 distinct values between 1 and 1000000");
        }
        if (z.isEmpty() || z.size() > 10 || hasNull(z) || new HashSet<>(z).size() != z.size()
                || z.stream().anyMatch(v -> v < 2 || v > 100)) {
            throw new InvalidRequestException("sizes", "must be 1 to 10 distinct values between 2 and 100");
        }
        if (r < 1 || r > 3) {
            throw new InvalidRequestException("repetitions", "must be between 1 and 3");
        }
        if (label != null && label.length() > 120) {
            throw new InvalidRequestException("label", "must be at most 120 characters");
        }
        if ((long) p.size() * s.size() * z.size() * r > MAX_RUNS) {
            throw new InvalidRequestException("sizes", "at most " + MAX_RUNS + " runs per batch");
        }
        List<DemandProfile> np = new ArrayList<>(p);
        np.sort(Comparator.comparingInt(Enum::ordinal));
        List<Long> ns = new ArrayList<>(s);
        ns.sort(null);
        List<Integer> nz = new ArrayList<>(z);
        nz.sort(null);
        boolean canonical = np.equals(CANONICAL_PROFILES) && ns.equals(CANONICAL_SEEDS) && nz.equals(CANONICAL_SIZES);
        String trimmed = label == null ? "" : label.trim();
        if (trimmed.isEmpty()) {
            trimmed = canonical ? CANONICAL_LABEL : SUBSET_LABEL;
        }
        return new ExperimentSpec(List.copyOf(np), List.copyOf(ns), List.copyOf(nz), r, trimmed);
    }

    private static boolean hasNull(List<?> values) {
        return values.stream().anyMatch(java.util.Objects::isNull);
    }

    public static ExperimentSpec canonical() {
        return of(null, null, null, null, null);
    }

    public boolean isCanonical() {
        return profiles.equals(CANONICAL_PROFILES) && seeds.equals(CANONICAL_SEEDS) && sizes.equals(CANONICAL_SIZES);
    }

    public int scenarioCount() {
        return profiles.size() * seeds.size() * sizes.size();
    }

    public int runCount() {
        return scenarioCount() * repetitions;
    }
}
