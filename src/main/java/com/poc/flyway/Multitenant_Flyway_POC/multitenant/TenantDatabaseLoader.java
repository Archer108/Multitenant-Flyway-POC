package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TenantDatabaseLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantDatabaseLoader.class);

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, String> tenantDatabaseMap = new ConcurrentHashMap<>();

    public TenantDatabaseLoader(DataSource primaryDataSource) {
        this.jdbcTemplate = new JdbcTemplate(primaryDataSource);
        loadTenantDatabases();
    }

    private void loadTenantDatabases() {
        String query =
                "SELECT DISTINCT tenant_id, connection_tx FROM bcpm_primary_schema.tenant_registry";
        List<Map<String, Object>> tenants = jdbcTemplate.queryForList(query);

        for (Map<String, Object> tenant : tenants) {
            String tenantId = (String) tenant.get("tenant_id");
            String dbUrl = (String) tenant.get("connection_tx");
            tenantDatabaseMap.put(tenantId, dbUrl);
            LOGGER.info("Loaded database connection for Tenant: {} -> {}", tenantId, dbUrl);
        }
    }

    public Map<String, String> getDatabaseMap() {
        log.info("📌 Current Tenant Database Mappings: {}", tenantDatabaseMap);
        return tenantDatabaseMap;
    }}
