package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.service.impl;

import com.adp.benefits.carrier.api.service.IClientCacheManagementService;
import com.adp.benefits.carrier.config.MultiTenantDataSource;
import com.adp.benefits.carrier.config.TenantDatabaseLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClientCacheManagementServiceImpl implements IClientCacheManagementService {

    private final TenantDatabaseLoader tenantDatabaseLoader;
    private final MultiTenantDataSource multiTenantDataSource;

    public ClientCacheManagementServiceImpl(TenantDatabaseLoader tenantDatabaseLoader,
                                            MultiTenantDataSource multiTenantDataSource) {
        this.tenantDatabaseLoader = tenantDatabaseLoader;
        this.multiTenantDataSource = multiTenantDataSource;
    }

    @Override
    public void refreshTenantCache() throws Exception {
        log.info("🔄 Refreshing tenant cache and multi-tenant datasource...");
        tenantDatabaseLoader.loadTenantDatabases();    // Reload mappings
        multiTenantDataSource.reload();                // Reload datasource registry
        log.info("✅ Tenant cache and datasource pool successfully refreshed.");
    }
}