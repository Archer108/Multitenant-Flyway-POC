package com.poc.flyway.Multitenant_Flyway_POC.appConfig;

import com.adp.benefits.carrier.util.DecryptionUtil;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MultiTenantDataSource extends AbstractRoutingDataSource {

    private final TenantDatabaseLoader tenantDatabaseLoader;

    // Map of clientId -> DataSource
    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();
    // Map of dbUrl -> singleton HikariDataSource to prevent duplicates
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Value("${spring.datasource.clients.username}")
    private String username;

    @Value("${spring.datasource.clients.password}")
    private String password;

    private boolean defaultDataSourceWarningLogged = false;

    public MultiTenantDataSource(TenantDatabaseLoader tenantDatabaseLoader) {
        this.tenantDatabaseLoader = tenantDatabaseLoader;
    }

    @PostConstruct
    public void initializeDataSources() throws Exception {
        reload(); // Initial load
    }

    public synchronized void reload() throws Exception {
        log.info("🔁 Reloading MultiTenantDataSource with latest tenant mappings...");

        Map<String, Map<String, Object>> tenantDatabases = tenantDatabaseLoader.getClientTenantMap();
        Map<Object, Object> newDataSources = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : tenantDatabases.entrySet()) {
            String clientId = entry.getKey();
            String dbUrl = (String) entry.getValue().get("dbUrl");

            DataSource ds = dataSourceCache.computeIfAbsent(dbUrl, url -> {
                try {
                    return createDataSource(url);
                } catch (Exception e) {
                    throw new RuntimeException("❌ Failed to create DataSource for " + url, e);
                }
            });

            newDataSources.put(clientId, ds);
            log.info("✅ Initialized datasource for client [{}] -> {}", clientId, dbUrl);
        }

        this.dataSources.clear();
        this.dataSources.putAll(newDataSources);
        this.setTargetDataSources(new HashMap<>(dataSources));
        this.setDefaultTargetDataSource(dataSources.values().stream().findFirst().orElseThrow());
        this.afterPropertiesSet();

        log.info("🚀 MultiTenantDataSource reload complete with {} datasources", dataSources.size());
    }

    private DataSource createDataSource(String dbUrl) throws Exception {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(username);
        ds.setPassword(DecryptionUtil.decrypt(password));
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setIdleTimeout(30000);
        ds.setPoolName("TenantPool-" + UUID.randomUUID());
        return ds;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenant = TenantContextHolder.getCurrentTenant();

        if (tenant == null) {
            if (!defaultDataSourceWarningLogged) {
                log.warn("⚠ No tenant set in context. Falling back to default DataSource.");
                defaultDataSourceWarningLogged = true;
            }
            return null;
        }

        Map<String, Map<String, Object>> tenantMap = tenantDatabaseLoader.getClientTenantMap();
        for (Map.Entry<String, Map<String, Object>> entry : tenantMap.entrySet()) {
            @SuppressWarnings("unchecked")
            Set<String> tenantIds = (Set<String>) entry.getValue().get("tenantIds");
            if (tenantIds.contains(tenant)) {
                log.debug("📌 Resolved tenant [{}] to client [{}]", tenant, entry.getKey());
                return entry.getKey();
            }
        }

        log.warn("⚠ Tenant [{}] not mapped to any client. Using default datasource.", tenant);
        return null;
    }
}