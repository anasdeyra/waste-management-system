package com.ase.wastems.config;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Start-up schema upgrade for databases created by earlier sprints (D-050). Hibernate 6.6 maps
 * string enums to native H2 ENUM columns and {@code ddl-auto: update} never changes an existing
 * column type, so new enum values would be rejected. This bean converts the affected columns to
 * VARCHAR and back-fills {@code evaluation_run.point_count}. The value-whitelist CHECK
 * constraints that old H2 ENUM columns left behind survive that conversion, so they are dropped
 * as well (new enum values, for instance {@code DEMO_RESET}, D-055). It depends on the entity
 * manager factory so that it runs after Hibernate's schema update and before any request.
 */
@Component
public class SchemaUpgrade implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public SchemaUpgrade(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void afterPropertiesSet() {
        upgrade();
    }

    public void upgrade() {
        if (!isH2()) {
            return;
        }
        convertEnumColumn("AUDIT_EVENT", "ACTION", 40);
        convertEnumColumn("AUDIT_EVENT", "ENTITY_TYPE", 30);
        convertEnumColumn("EVALUATION_RUN", "SCENARIO_KIND", 20);
        convertEnumColumn("EVALUATION_METHOD_RESULT", "METHOD", 20);
        convertEnumColumn("TOUR", "ROUTE_METHOD", 20);
        int filled = jdbc.update("UPDATE EVALUATION_RUN r SET POINT_COUNT = "
                + "(SELECT COUNT(*) FROM EVALUATION_STOP s WHERE s.RUN_ID = r.ID) WHERE POINT_COUNT IS NULL");
        if (filled > 0) {
            log.info("Schema upgrade: point_count filled for {} evaluation runs", filled);
        }
        int dropped = dropEnumWhitelistChecks();
        if (dropped > 0) {
            log.info("Schema upgrade: {} value-whitelist check constraints dropped", dropped);
        }
    }

    /** Names come from constants or tests only, never from user input. */
    public boolean convertEnumColumn(String table, String column, int length) {
        var types = jdbc.queryForList(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' "
                        + "AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class, table, column);
        if (types.isEmpty() || !"ENUM".equals(types.get(0))) {
            return false;
        }
        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DATA TYPE VARCHAR(" + length + ")");
        log.info("Schema upgrade: {}.{} converted from ENUM to VARCHAR({})", table, column, length);
        return true;
    }

    /**
     * The repository defines no CHECK constraints of its own, and H2 encodes NOT NULL as a CHECK
     * as well, so exactly the legacy {@code VALUE IN (...)} whitelists are dropped — across the
     * whole schema, because a later sprint may add a new value to any stored enum while an old
     * database upgrades. Clause normalization keeps the match readable, not the source of truth.
     */
    public int dropEnumWhitelistChecks() {
        var rows = jdbc.query(
                "SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, k.CHECK_CLAUSE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS c "
                        + "JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS k ON k.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA "
                        + "AND k.CONSTRAINT_NAME = c.CONSTRAINT_NAME "
                        + "WHERE c.TABLE_SCHEMA = 'PUBLIC' AND c.CONSTRAINT_TYPE = 'CHECK'",
                (rs, i) -> new String[] {rs.getString(1), rs.getString(2), rs.getString(3)});
        int dropped = 0;
        for (String[] row : rows) {
            String clause = row[2] == null ? "" : String.valueOf(row[2]).toUpperCase().replace(" ", "");
            if (!clause.contains("IN(")) {
                continue;
            }
            jdbc.execute("ALTER TABLE " + row[0] + " DROP CONSTRAINT " + row[1]);
            log.info("Schema upgrade: {}.{} value-whitelist check dropped ({})", row[0], row[1], row[2]);
            dropped++;
        }
        return dropped;
    }

    private boolean isH2() {
        try (Connection c = dataSource.getConnection()) {
            return "H2".equals(c.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            return false;
        }
    }
}
