package com.poc.flyway.Multitenant_Flyway_POC.appConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@DependsOn("primaryFlywayInitializer")
public class TenantDatabaseLoader {

    private final JdbcTemplate jdbcTemplate;

    private final Map<String, Map<String, Object>> clientTenantMap = new ConcurrentHashMap<>();
    private final Map<String, String> oidTenantMap = new ConcurrentHashMap<>();

    public TenantDatabaseLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        loadTenantDatabases();
    }

    public synchronized void loadTenantDatabases() {
        log.info("🔄 Reloading tenant database mappings...");

        clientTenantMap.clear();
        oidTenantMap.clear();

        String query = """
            SELECT tr.tenant_id, tr.connection_tx, c.client_id, c.oid
            FROM bcpm_primary_schema.tenant_registry tr
            LEFT JOIN bcpm_primary_schema.client c ON tr.tenant_id = c.tenant_id
        """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

        for (Map<String, Object> result : results) {
            String tenantId = getString(result.get("tenant_id"));
            String dbUrl = getString(result.get("connection_tx"));
            String clientId = getString(result.get("client_id"));
            String oid = getString(result.get("oid"));

            if (clientId != null) {
                clientTenantMap.computeIfAbsent(clientId, k -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("dbUrl", dbUrl);
                    map.put("tenantIds", new HashSet<String>());
                    return map;
                });
                @SuppressWarnings("unchecked")
                Set<String> tenantIds = (Set<String>) clientTenantMap.get(clientId).get("tenantIds");
                tenantIds.add(tenantId);
            }

            if (oid != null) {
                oidTenantMap.put(oid, tenantId);
            }
        }

        log.info("✅ Loaded {} client-tenant mappings", clientTenantMap.size());
    }

    private String getString(Object value) {
        return value != null ? value.toString() : null;
    }

    public Map<String, Map<String, Object>> getClientTenantMap() {
        return Collections.unmodifiableMap(clientTenantMap);
    }

    public String getTenantIdByOid(String oid) {
        return oidTenantMap.get(oid);
    }
}