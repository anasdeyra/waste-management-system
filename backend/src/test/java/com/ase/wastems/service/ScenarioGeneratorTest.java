package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ase.wastems.config.ServiceAreaProperties;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScenarioGeneratorTest {

    static final Mn01ScenarioPool POOLER = new Mn01ScenarioPool(new ServiceAreaProperties(-74.30, -73.65, 40.45, 40.95));
    static final List<PoolBasket> POOL = POOLER.baskets();

    private static List<String> ids(List<PoolBasket> l) {
        return l.stream().map(PoolBasket::basketId).toList();
    }

    @Test
    void poolIsTheSortedDsnyBaskets() {
        assertThat(POOL).hasSize(238);
        assertThat(POOL.get(0).basketId()).isEqualTo("10110006");
        assertThat(POOL.get(237).basketId()).isEqualTo("10130352");
        assertThat(ids(POOL)).isSorted();
        assertThat(POOLER.basketSnapshotId()).isEqualTo("8znf-7b2c:449:a0737237e370");
        assertThat(POOLER.depotSnapshotId()).isEqualTo("xw3j-2yxf:1:207851c2e229");
    }

    @Test
    void goldenLocations() {
        assertThat(ids(ScenarioGenerator.sample(POOL, 1, 10))).containsExactly("10130030", "10130234", "10130154",
                "10130100", "10130238", "10130246", "10130245", "10130099", "10130034", "10130094");
        assertThat(ids(ScenarioGenerator.sample(POOL, 2, 10))).containsExactly("10130309", "10130154", "10130128",
                "10130100", "10130312", "10130114", "10110006", "10130098", "10130282", "10130333");
    }

    @Test
    void goldenDemand() {
        assertThat(ScenarioGenerator.demandsKg(DemandProfile.UNIFORM_LOW, 1, 10))
                .containsExactly(21, 23, 12, 17, 26, 15, 29, 11, 19, 29);
        assertThat(ScenarioGenerator.demandsKg(DemandProfile.UNIFORM_HIGH, 1, 10))
                .containsExactly(41, 44, 52, 44, 48, 58, 48, 33, 43, 41);
        assertThat(ScenarioGenerator.demandsKg(DemandProfile.HOTSPOT, 1, 10))
                .containsExactly(19, 15, 6, 10, 91, 69, 5, 6, 15, 77);
    }

    @Test
    void deterministic() {
        assertThat(ScenarioGenerator.sample(POOL, 7, 100)).isEqualTo(ScenarioGenerator.sample(POOL, 7, 100));
        assertThat(ScenarioGenerator.demandsKg(DemandProfile.HOTSPOT, 7, 100))
                .isEqualTo(ScenarioGenerator.demandsKg(DemandProfile.HOTSPOT, 7, 100));
    }

    @Test
    void withoutReplacement() {
        assertThat(ids(ScenarioGenerator.sample(POOL, 3, 100)).stream().distinct().count()).isEqualTo(100);
        Set<String> all = ids(ScenarioGenerator.sample(POOL, 3, 238)).stream().collect(Collectors.toSet());
        assertThat(all).isEqualTo(Set.copyOf(ids(POOL)));
    }

    @Test
    void sizesAreNested() {
        assertThat(ScenarioGenerator.sample(POOL, 5, 10))
                .isEqualTo(ScenarioGenerator.sample(POOL, 5, 25).subList(0, 10));
        for (DemandProfile p : DemandProfile.values()) {
            assertThat(ScenarioGenerator.demandsKg(p, 5, 10))
                    .isEqualTo(ScenarioGenerator.demandsKg(p, 5, 25).subList(0, 10));
        }
    }

    @Test
    void seedsDiffer() {
        assertThat(ScenarioGenerator.sample(POOL, 1, 25)).isNotEqualTo(ScenarioGenerator.sample(POOL, 2, 25));
    }

    @Test
    void demandRangesAndLargestTotal() {
        int largest = 0;
        for (long seed = 1; seed <= 5; seed++) {
            for (DemandProfile p : DemandProfile.values()) {
                List<Integer> kg = ScenarioGenerator.demandsKg(p, seed, 100);
                switch (p) {
                    case UNIFORM_LOW -> assertThat(kg).allMatch(v -> v >= 10 && v <= 30);
                    case UNIFORM_HIGH -> assertThat(kg).allMatch(v -> v >= 30 && v <= 60);
                    case HOTSPOT -> {
                        assertThat(kg).allMatch(v -> (v >= 5 && v <= 20) || (v >= 60 && v <= 120));
                        assertThat(kg).anyMatch(v -> v >= 60);
                    }
                }
                largest = Math.max(largest, kg.stream().mapToInt(Integer::intValue).sum());
            }
        }
        assertThat(largest).isEqualTo(4639);
    }

    @Test
    void keysAndBounds() {
        assertThat(ScenarioGenerator.scenarioKey(DemandProfile.UNIFORM_LOW, 1, 10)).isEqualTo("mn01-UNIFORM_LOW-s1-n10");
        assertThatThrownBy(() -> ScenarioGenerator.sample(POOL, 1, 239)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ScenarioGenerator.sample(POOL, 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
