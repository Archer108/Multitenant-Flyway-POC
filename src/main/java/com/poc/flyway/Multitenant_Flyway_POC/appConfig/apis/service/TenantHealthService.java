package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.service;

import com.adp.benefits.carrier.config.MultiTenantDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TenantHealthService {

    private final MultiTenantDataSource multiTenantDataSource;

    public TenantHealthService(MultiTenantDataSource multiTenantDataSource) {
        this.multiTenantDataSource = multiTenantDataSource;
    }

    public List<Map<String, Object>> getTenantHealthStatus() {
        List<Map<String, Object>> healthStatusList = new ArrayList<>();

        // Iterate over each client (tenant group) in the resolved DataSources map
        Map<Object, Object> dsMap = multiTenantDataSource.getTenantDataSources();
        dsMap.forEach((clientId, ds) -> {
            Map<String, Object> status = new HashMap<>();
            status.put("clientId", clientId);

            try (Connection connection = ((DataSource) ds).getConnection()) {
                if (connection.isValid(2)) {
                    status.put("status", "UP");
                    status.put("message", "Connection valid");
                } else {
                    status.put("status", "DOWN");
                    status.put("message", "Connection invalid");
                }
            } catch (Exception e) {
                status.put("status", "DOWN");
                status.put("message", e.getMessage());
                log.error("Health check failed for client {}: {}", clientId, e.getMessage(), e);
            }
            healthStatusList.add(status);
        });

        return healthStatusList;
    }
}
