package com.poc.flyway.Multitenant_Flyway_POC.appConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiTenantDataSourceConfig {

    private final TenantDatabaseLoader tenantDatabaseLoader;

    public MultiTenantDataSourceConfig(TenantDatabaseLoader tenantDatabaseLoader) {
        this.tenantDatabaseLoader = tenantDatabaseLoader;
    }

    @Bean(name = "multiTenantDataSource")
    public MultiTenantDataSource multiTenantDataSource() {
        return new MultiTenantDataSource(tenantDatabaseLoader);
    }
}
