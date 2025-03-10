package com.poc.flyway.Multitenant_Flyway_POC.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlywayConfig {

    private final CustomFlywayProperties customFlywayProperties;

    @Bean(initMethod = "migrate")
    public Flyway primaryFlyway(@Qualifier("primaryDataSource") Object ignored) {
        CustomFlywayProperties.FlywaySettings settings = customFlywayProperties.getPrimary();
        log.info("Migrating primary datasource using URL: {}", settings.getUrl());
        return Flyway.configure()
                .dataSource(settings.getUrl(), settings.getUser(), settings.getPassword())
                .baselineOnMigrate(settings.isBaselineOnMigrate())
                .baselineVersion(settings.getBaselineVersion())
                .outOfOrder(settings.isOutOfOrder())
                .locations(settings.getLocations())
                .table(settings.getTable())
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway configFlyway(@Qualifier("configDataSource") Object ignored) {
        CustomFlywayProperties.FlywaySettings settings = customFlywayProperties.getConfig();
        log.info("Migrating config datasource using URL: {}", settings.getUrl());
        return Flyway.configure()
                .dataSource(settings.getUrl(), settings.getUser(), settings.getPassword())
                .baselineOnMigrate(settings.isBaselineOnMigrate())
                .baselineVersion(settings.getBaselineVersion())
                .outOfOrder(settings.isOutOfOrder())
                .locations(settings.getLocations())
                .table(settings.getTable())
                .load();
    }
}
