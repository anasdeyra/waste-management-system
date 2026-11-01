package com.ase.wastems.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.repository.EvaluationRunRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SchemaUpgradeTest {

    @Autowired SchemaUpgrade upgrade;
    @Autowired JdbcTemplate jdbc;
    @Autowired EvaluationRunRepository runs;

    private String type(String table, String column) {
        return jdbc.queryForObject(
                "select data_type from information_schema.columns where table_schema='PUBLIC' and table_name=? and column_name=?",
                String.class, table, column);
    }

    @Test
    void enumColumnsAreVarcharOnANewDatabase() {
        assertThat(type("AUDIT_EVENT", "ACTION")).isEqualTo("CHARACTER VARYING");
        assertThat(type("AUDIT_EVENT", "ENTITY_TYPE")).isEqualTo("CHARACTER VARYING");
        assertThat(type("EVALUATION_RUN", "SCENARIO_KIND")).isEqualTo("CHARACTER VARYING");
        assertThat(type("EVALUATION_BATCH", "KIND")).isEqualTo("CHARACTER VARYING");
        assertThat(type("EVALUATION_BATCH", "STATUS")).isEqualTo("CHARACTER VARYING");
    }

    @Test
    void convertsAnEnumColumnKeepingDataAndNotNull() {
        try {
            jdbc.execute("CREATE TABLE S9_ENUM_PROBE (ID INT PRIMARY KEY, KIND ENUM('A','B') NOT NULL)");
            jdbc.update("insert into S9_ENUM_PROBE values (1,'A')");
            assertThat(upgrade.convertEnumColumn("S9_ENUM_PROBE", "KIND", 20)).isTrue();
            assertThat(type("S9_ENUM_PROBE", "KIND")).isEqualTo("CHARACTER VARYING");
            assertThat(jdbc.queryForObject(
                            "select character_maximum_length from information_schema.columns where table_name='S9_ENUM_PROBE' and column_name='KIND'",
                            Integer.class))
                    .isEqualTo(20);
            assertThat(jdbc.queryForObject(
                            "select is_nullable from information_schema.columns where table_name='S9_ENUM_PROBE' and column_name='KIND'",
                            String.class))
                    .isEqualTo("NO");
            assertThat(jdbc.queryForObject("select kind from S9_ENUM_PROBE where id=1", String.class)).isEqualTo("A");
            jdbc.update("insert into S9_ENUM_PROBE values (2,'C')");
            assertThat(upgrade.convertEnumColumn("S9_ENUM_PROBE", "KIND", 20)).isFalse();
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS S9_ENUM_PROBE");
        }
    }

    @Test
    void upgradeIsIdempotentAndBackfillsPointCount() {
        List<EvaluationStop> stops = List.of(
                new EvaluationStop(1, UUID.randomUUID(), null, null, "a", -74.0, 40.7, BigDecimal.TEN),
                new EvaluationStop(2, UUID.randomUUID(), null, null, "b", -74.0, 40.71, BigDecimal.TEN));
        EvaluationRun run = EvaluationRun.forTourDraft(UUID.randomUUID(), 1, "planner1", Instant.now(), "v", "e",
                List.of("s"), "Depot", -74.0, 40.7, null, BigDecimal.valueOf(100), 600, stops);
        UUID id = runs.save(run).getId();
        try {
            jdbc.update("update evaluation_run set point_count = null where id = ?", id);
            upgrade.upgrade();
            upgrade.upgrade();
            assertThat(jdbc.queryForObject("select point_count from evaluation_run where id = ?", Integer.class, id))
                    .isEqualTo(2);
        } finally {
            runs.deleteById(id);
        }
    }
}
