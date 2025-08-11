package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.interfaces.IClientCacheManagementService;
import com.adp.benefits.carrier.config.database.MultiTenantDataSource;
import com.adp.benefits.carrier.config.database.TenantDatabaseLoader;
import org.springframework.stereotype.Service;

@Service
public class ClientCacheManagementServiceImpl implements IClientCacheManagementService {

    private final TenantDatabaseLoader tenantDatabaseLoader;
    private final MultiTenantDataSource multiTenantDataSource;

    public ClientCacheManagementServiceImpl(
            TenantDatabaseLoader tenantDatabaseLoader,
            MultiTenantDataSource multiTenantDataSource) {
        this.tenantDatabaseLoader = tenantDatabaseLoader;
        this.multiTenantDataSource = multiTenantDataSource;
    }

    @Override
    public void refreshTenantCache() throws Exception {
        tenantDatabaseLoader.loadTenantDatabases();
        multiTenantDataSource.reload();
    }
}
