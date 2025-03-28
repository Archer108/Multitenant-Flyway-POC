package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.controller;

import com.adp.benefits.carrier.api.service.TenantHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TenantHealthController {

    private final TenantHealthService tenantHealthService;

    public TenantHealthController(TenantHealthService tenantHealthService) {
        this.tenantHealthService = tenantHealthService;
    }

    @GetMapping("/api/health/tenants")
    public ResponseEntity<List<Map<String, Object>>> getTenantHealth() {
        List<Map<String, Object>> healthStatus = tenantHealthService.getTenantHealthStatus();
        return ResponseEntity.ok(healthStatus);
    }
}