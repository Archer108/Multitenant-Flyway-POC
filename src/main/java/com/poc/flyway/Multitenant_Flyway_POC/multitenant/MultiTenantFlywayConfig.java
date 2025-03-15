package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

import com.adp.benefits.carrier.util.DecryptionUtil;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MultiTenantFlywayConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantFlywayConfig.class);
    private final JdbcTemplate jdbcTemplate;

    public MultiTenantFlywayConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${spring.datasource.primary.username}")
    private String username;

    @Value("${spring.datasource.primary.password}")
    private String password;

    @Bean
    public ApplicationRunner runMultiTenantFlyway() {
        return args -> {
            // Wait for primary migration to complete
            waitForPrimaryMigration();

            String query = "SELECT tenant_id, connection_tx, schema_tx FROM bcpm_primary_schema.tenant_registry";
            List<Map<String, Object>> tenants = jdbcTemplate.queryForList(query);

            String decryptedPassword = DecryptionUtil.decrypt(password);

            for (Map<String, Object> tenant : tenants) {
                String tenantId = (String) tenant.get("tenant_id");
                String dbUrl = (String) tenant.get("connection_tx");
                String schema = (String) tenant.get("schema_tx");


                LOGGER.info("🔄 Running Flyway Migration for Tenant: {} ({})", tenantId, dbUrl);

                Flyway flyway = Flyway.configure()
                        .dataSource(dbUrl, username, decryptedPassword)
                        .baselineOnMigrate(true)
                        .outOfOrder(true)
                        .schemas(schema)
                        .table("client_schema_history")
                        .locations("classpath:db/migration/client")
                        .load();

                flyway.migrate();
                logMigrationDetails(flyway, tenantId);
                LOGGER.info("✅ Flyway Migration Completed for Tenant: {}", tenantId);
            }
        };
    }

    private void logMigrationDetails(Flyway flyway, String tenantId) {
        MigrationInfoService migrationInfoService = flyway.info();

        LOGGER.info("📜 Applied Migrations for Tenant: {}", tenantId);
        MigrationInfo[] appliedMigrations = migrationInfoService.applied();
        if (appliedMigrations.length == 0) {
            LOGGER.warn("⚠ No migrations applied.");
        }
        for (MigrationInfo migration : appliedMigrations) {
            LOGGER.info("✅ Version: {} | Description: {} | Execution Time: {}ms | Script: {}",
                    migration.getVersion(),
                    migration.getDescription(),
                    migration.getExecutionTime(),
                    migration.getScript());
        }

        LOGGER.info("⏳ Pending Migrations for Tenant: {}", tenantId);
        MigrationInfo[] pendingMigrations = migrationInfoService.pending();
        if (pendingMigrations.length == 0) {
            LOGGER.info("✅ No pending migrations.");
        }
        for (MigrationInfo migration : pendingMigrations) {
            LOGGER.warn("⏳ Pending - Version: {} | Description: {} | Script: {}",
                    migration.getVersion(),
                    migration.getDescription(),
                    migration.getScript());
        }
    }

    private void waitForPrimaryMigration() {
        LOGGER.info("⏳ Waiting for Primary Database Migration...");
        boolean tableExists = false;
        int attempts = 0;
        int maxAttempts = 10;

        while (!tableExists && attempts < maxAttempts) {
            try {
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM bcpm_primary_schema.tenant_registry", Integer.class);
                tableExists = true;
            } catch (Exception e) {
                LOGGER.warn("⏳ Still waiting... Retrying in 2 seconds.");
                attempts++;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }

        if (tableExists) {
            LOGGER.info("✅ Primary Migration Completed, Proceeding with Client Migrations.");
        } else {
            LOGGER.error("❌ ERROR: Primary Migration did not complete within expected time.");
        }
    }
}
