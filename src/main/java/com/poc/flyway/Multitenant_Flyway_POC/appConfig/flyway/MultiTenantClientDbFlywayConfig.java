package com.poc.flyway.Multitenant_Flyway_POC.appConfig.flyway;

import com.adp.benefits.carrier.util.DecryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MultiTenantClientDbFlywayConfig {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.primary.username}")
    private String username;

    @Value("${spring.datasource.primary.password}")
    private String password;

    public MultiTenantClientDbFlywayConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    public ApplicationRunner runMultiTenantFlyway() {
        return args -> {
            waitForPrimaryMigration();

            String query = """
                SELECT tenant_id, connection_tx, schema_tx
                FROM bcpm_primary_schema.tenant_registry
            """;

            List<Map<String, Object>> tenants = jdbcTemplate.queryForList(query);
            String decryptedPassword = DecryptionUtil.decrypt(password);

            for (Map<String, Object> tenant : tenants) {
                String tenantId = (String) tenant.get("tenant_id");
                String dbUrl = (String) tenant.get("connection_tx");
                String schema = (String) tenant.get("schema_tx");

                log.info("🔄 Starting Flyway migration for tenant: {} ({})", tenantId, dbUrl);

                try {
                    Flyway flyway = Flyway.configure()
                            .dataSource(dbUrl, username, decryptedPassword)
                            .schemas(schema)
                            .table("client_schema_history")
                            .baselineOnMigrate(true)
                            .outOfOrder(true)
                            .locations("classpath:db/migration/client")
                            .load();

                    flyway.migrate();
                    logMigrationDetails(flyway, tenantId);

                    log.info("✅ Flyway migration completed for tenant: {}", tenantId);
                } catch (Exception e) {
                    log.error("❌ Migration failed for tenant {}: {}", tenantId, e.getMessage(), e);
                }
            }
        };
    }

    private void logMigrationDetails(Flyway flyway, String tenantId) {
        MigrationInfoService infoService = flyway.info();

        MigrationInfo[] applied = infoService.applied();
        MigrationInfo[] pending = infoService.pending();

        if (applied.length > 0) {
            log.info("📜 Applied Migrations for tenant [{}]:", tenantId);
            for (MigrationInfo m : applied) {
                log.info("✅ [{}] {} | {}ms | {}", m.getVersion(), m.getDescription(), m.getExecutionTime(), m.getScript());
            }
        }

        if (pending.length > 0) {
            log.warn("⏳ Pending Migrations for tenant [{}]:", tenantId);
            for (MigrationInfo m : pending) {
                log.warn("⏳ [{}] {} | {}", m.getVersion(), m.getDescription(), m.getScript());
            }
        } else {
            log.info("✅ No pending migrations for tenant [{}]", tenantId);
        }
    }

    private void waitForPrimaryMigration() {
        log.info("⏳ Waiting for primary schema readiness...");

        int maxAttempts = 10;
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bcpm_primary_schema.tenant_registry", Integer.class);
                log.info("✅ Primary schema ready. Proceeding with tenant migrations.");
                return;
            } catch (Exception e) {
                log.warn("⏳ Waiting for primary schema... retrying in 2s. Attempt {}", attempts + 1);
                attempts++;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    log.error("❌ Thread interrupted while waiting for primary schema.");
                    return;
                }
            }
        }

        log.error("❌ Primary migration not ready after {} attempts. Skipping client migrations.", maxAttempts);
    }
}