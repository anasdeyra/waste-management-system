package com.ase.wastems.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Seeded scenario generator mn01-v1 (D-016, D-022, D-050): locations by a Fisher-Yates prefix of
 * the sorted real-basket pool, demand per profile from an independent random stream. Same
 * profile, seed and size always give the same scenario.
 */
public final class ScenarioGenerator {

    public static final String VERSION = "mn01-v1";

    private ScenarioGenerator() {}

    public static List<PoolBasket> sample(List<PoolBasket> sortedPool, long seed, int size) {
        if (size < 1 || size > sortedPool.size()) {
            throw new IllegalArgumentException("size must be between 1 and " + sortedPool.size());
        }
        Random loc = new Random(seed);
        List<PoolBasket> list = new ArrayList<>(sortedPool);
        for (int i = list.size() - 1; i > 0; i--) {
            int j = loc.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
        return List.copyOf(list.subList(0, size));
    }

    public static List<Integer> demandsKg(DemandProfile profile, long seed, int size) {
        Random dem = new Random(31L * seed + profile.code());
        List<Integer> kg = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            kg.add(profile.drawKg(dem));
        }
        return List.copyOf(kg);
    }

    public static String scenarioKey(DemandProfile profile, long seed, int size) {
        return "mn01-" + profile.name() + "-s" + seed + "-n" + size;
    }
}
