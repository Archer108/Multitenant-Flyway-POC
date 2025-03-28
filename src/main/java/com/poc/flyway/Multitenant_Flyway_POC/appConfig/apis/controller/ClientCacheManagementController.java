package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.controller;

import com.adp.benefits.carrier.api.interfaces.IClientCacheManagement;
import com.adp.benefits.carrier.api.service.IClientCacheManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientCacheManagementController implements IClientCacheManagement {

    private final IClientCacheManagementService clientCacheManagementService;

    public ClientCacheManagementController(IClientCacheManagementService clientCacheManagementService) {
        this.clientCacheManagementService = clientCacheManagementService;
    }

    @Override
    public ResponseEntity<String> refreshTenantCache() {
        try {
            clientCacheManagementService.refreshTenantCache();
            return ResponseEntity.ok("Tenant cache refreshed and database connections reloaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while refreshing the tenant cache or reloading database connections: " + e.getMessage());
        }
    }
}
