package com.comandago.api.shared.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Repara el historial Flyway en BDs que ya tenían el esquema V1–V10 sin flyway_schema_history
 * (p. ej. Supabase). Solo activo con app.flyway.repair-baseline=true.
 */
@Configuration
@ConditionalOnProperty(name = "app.flyway.repair-baseline", havingValue = "true")
public class FlywaySupabaseRepairConfig {

    private static final MigrationVersion BASELINE_TARGET = MigrationVersion.fromVersion("10");

    @Bean
    FlywayMigrationStrategy supabaseFlywayMigrationStrategy() {
        return flyway -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(flyway.getConfiguration().getDataSource());
            if (needsHistoryReset(flyway, jdbcTemplate)) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS flyway_schema_history");
                flyway.baseline();
            }
            flyway.migrate();
        };
    }

    private boolean needsHistoryReset(Flyway flyway, JdbcTemplate jdbcTemplate) {
        if (!historyTableExists(jdbcTemplate)) {
            return schemaHasExistingTables(jdbcTemplate);
        }
        var current = flyway.info().current();
        if (current == null || current.getVersion() == null) {
            return schemaHasExistingTables(jdbcTemplate);
        }
        return current.getVersion().compareTo(BASELINE_TARGET) < 0;
    }

    private boolean historyTableExists(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'flyway_schema_history'
                """,
                Integer.class);
        return count != null && count > 0;
    }

    private boolean schemaHasExistingTables(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('usuarios', 'pedidos', 'pagos')
                """,
                Integer.class);
        return count != null && count >= 3;
    }
}
