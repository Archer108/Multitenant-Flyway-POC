package com.poc.flyway.Multitenant_Flyway_POC.appConfig.flyway;

import com.adp.benefits.carrier.util.DecryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PrimaryDbFlywayConfig {

    @Value("${spring.datasource.primary.url}")
    private String url;

    @Value("${spring.datasource.primary.username}")
    private String username;

    @Value("${spring.datasource.primary.password}")
    private String password;

    @Bean(name = "primaryFlywayInitializer")
    public Flyway primaryFlyway() throws Exception {
        log.info("🔄 Starting Flyway migration for primary database...");

        String decryptedPassword = DecryptionUtil.decrypt(password);

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, decryptedPassword)
                .schemas("bcpm_primary_schema")
                .table("primary_schema_history")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .locations("classpath:db/migration/primary")
                .load();

        try {
            flyway.migrate();
            logMigrationDetails(flyway);
            log.info("✅ Primary database migration completed successfully.");
        } catch (Exception e) {
            log.error("❌ Primary database migration failed: {}", e.getMessage(), e);
            throw e;
        }

        return flyway;
    }

    private void logMigrationDetails(Flyway flyway) {
        MigrationInfoService infoService = flyway.info();

        MigrationInfo[] applied = infoService.applied();
        MigrationInfo[] pending = infoService.pending();

        if (applied.length > 0) {
            log.info("📜 Applied Migrations for primary DB:");
            for (MigrationInfo m : applied) {
                log.info("✅ [{}] {} | {}ms | {}", m.getVersion(), m.getDescription(), m.getExecutionTime(), m.getScript());
            }
        } else {
            log.warn("⚠ No migrations have been applied yet.");
        }

        if (pending.length > 0) {
            log.warn("⏳ Pending Migrations for primary DB:");
            for (MigrationInfo m : pending) {
                log.warn("⏳ [{}] {} | {}", m.getVersion(), m.getDescription(), m.getScript());
            }
        } else {
            log.info("✅ No pending migrations for primary DB.");
        }
    }
}