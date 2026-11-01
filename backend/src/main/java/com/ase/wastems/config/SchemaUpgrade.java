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
 * VARCHAR and back-fills {@code evaluation_run.point_count}. It depends on the entity manager
 * factory so that it runs after Hibernate's schema update and before any request.
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
        int filled = jdbc.update("UPDATE EVALUATION_RUN r SET POINT_COUNT = "
                + "(SELECT COUNT(*) FROM EVALUATION_STOP s WHERE s.RUN_ID = r.ID) WHERE POINT_COUNT IS NULL");
        if (filled > 0) {
            log.info("Schema upgrade: point_count filled for {} evaluation runs", filled);
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

    private boolean isH2() {
        try (Connection c = dataSource.getConnection()) {
            return "H2".equals(c.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            return false;
        }
    }
}
