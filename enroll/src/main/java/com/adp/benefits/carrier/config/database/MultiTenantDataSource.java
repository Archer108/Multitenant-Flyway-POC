package com.adp.benefits.carrier.config.database;

import com.adp.benefits.carrier.exceptions.DataSourceCreationException;
import com.adp.benefits.carrier.util.DecryptionUtil;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MultiTenantDataSource extends AbstractRoutingDataSource {

    private final TenantDatabaseLoader tenantDatabaseLoader;
    private static final String TENANT_ID = "tenantId";

    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();
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
    public void initializeDataSources() {
        reload();
    }

    public synchronized void reload() {
        log.info("🔄 Reloading MultiTenantDataSource with latest tenant mappings...");

        Map<String, Map<String, Object>> tenantDatabases =
                tenantDatabaseLoader.getClientTenantMap();
        Map<Object, Object> newDataSources = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : tenantDatabases.entrySet()) {
            String clientId = entry.getKey();
            String dbUrl = (String) entry.getValue().get("dbUrl");

            DataSource ds =
                    dataSourceCache.computeIfAbsent(
                            dbUrl,
                            url -> {
                                try {
                                    return createDataSource(url);
                                } catch (DataSourceCreationException e) {
                                    throw new DataSourceCreationException(
                                            "❌ Failed to create DataSource for " + url, e);
                                }
                            });

            newDataSources.put(clientId, ds);
            log.info("✅ Initialized datasource for client [{}] -> [{}]", clientId, dbUrl);
        }

        this.dataSources.clear();
        this.dataSources.putAll(newDataSources);
        this.setTargetDataSources(new HashMap<>(dataSources));
        this.setDefaultTargetDataSource(dataSources.values().stream().findFirst().orElseThrow());
        this.afterPropertiesSet();

        log.info("✨ MultiTenantDataSource reload complete with {} datasources", dataSources.size());
    }

    private DataSource createDataSource(String dbUrl) {
        HikariDataSource ds = new HikariDataSource();
        boolean initialized = false;
        try {
            ds.setJdbcUrl(dbUrl);
            ds.setUsername(username);
            ds.setPassword(DecryptionUtil.decrypt(password));
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setMaximumPoolSize(10);
            ds.setMinimumIdle(2);
            ds.setIdleTimeout(30000);
            ds.setPoolName("TenantPool-" + UUID.randomUUID());
            initialized = true;
            return ds;
        } finally {
            if (!initialized) {
                ds.close();
            }
        }
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenant = MDC.get(TENANT_ID);
        log.debug("🔍 Looking up datasource for tenant. TenantId = [{}]", tenant);

        if (tenant == null) {
            if (!defaultDataSourceWarningLogged) {
                log.warn("⚠️ No tenant set in context. Falling back to default DataSource.");
                defaultDataSourceWarningLogged = true;
            }
            return null;
        }

        Map<String, Map<String, Object>> tenantMap = tenantDatabaseLoader.getClientTenantMap();
        log.debug(
                "📋 Available tenant mappings: {}",
                tenantMap.entrySet().stream()
                        .map(e -> e.getKey() + " -> " + e.getValue().get("tenantIds"))
                        .collect(Collectors.joining(", ")));

        for (Map.Entry<String, Map<String, Object>> entry : tenantMap.entrySet()) {
            String clientId = entry.getKey();
            @SuppressWarnings("unchecked")
            Set<String> tenantIds = (Set<String>) entry.getValue().get("tenantIds");

            if (tenantIds == null) {
                log.warn(
                        "⚠️ No tenant IDs found for client [{}]. Using default datasource.",
                        clientId);
                continue;
            }

            if (tenantIds.contains(tenant)) {
                log.debug(
                        "➡️ Routing request for tenant [{}] to client datasource [{}]",
                        tenant,
                        clientId);
                return clientId;
            } else {
                log.trace("❌ Client [{}] does not match tenant [{}]", clientId, tenant);
            }
        }

        log.warn("⚠️ Tenant [{}] not mapped to any client -> Using default datasource.", tenant);
        return null;
    }

    @PreDestroy
    public void closeDataSources() {
        for (Object dataSource : dataSources.values()) {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        }
    }
}
