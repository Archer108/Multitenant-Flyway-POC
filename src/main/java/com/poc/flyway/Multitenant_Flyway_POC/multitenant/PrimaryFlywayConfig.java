package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

import com.adp.benefits.carrier.util.DecryptionUtil;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrimaryFlywayConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryFlywayConfig.class);

    @Value("${spring.datasource.primary.url}")
    private String url;

    @Value("${spring.datasource.primary.username}")
    private String username;

    @Value("${spring.datasource.primary.password}")
    private String password;

    @Bean
    public Flyway primaryFlyway() throws Exception {
        LOGGER.info("🔄 Running Flyway Migration for Primary Database...");
        String decryptedPassword = DecryptionUtil.decrypt(password);
        Flyway flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .table("primary_schema_history")
                .locations("classpath:db/migration/primary")
                .dataSource(url, username, decryptedPassword)
                .load();

        flyway.migrate();
        logMigrationDetails(flyway, "Primary Database");
        LOGGER.info("✅ Primary Database Migration Completed.");
        return flyway;
    }

    private void logMigrationDetails(Flyway flyway, String databaseName) {
        MigrationInfoService migrationInfoService = flyway.info();

        LOGGER.info("📜 Applied Migrations for {}", databaseName);
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

        LOGGER.info("⏳ Pending Migrations for {}", databaseName);
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
}