package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

import com.adp.benefits.carrier.util.DecryptionUtil;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MultiTenantDataSource extends AbstractRoutingDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantDataSource.class);

    private final TenantDatabaseLoader tenantDatabaseLoader;
    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    @Value("${spring.datasource.clients.username}")
    private String username;

    @Value("${spring.datasource.clients.password}")
    private String password;

    public MultiTenantDataSource(TenantDatabaseLoader tenantDatabaseLoader) {
        this.tenantDatabaseLoader = tenantDatabaseLoader;
    }

    @PostConstruct
    public void initializeDataSources() throws Exception {
        Map<String, String> tenantDatabases = tenantDatabaseLoader.getDatabaseMap();

        for (Map.Entry<String, String> entry : tenantDatabases.entrySet()) {
            String tenantId = entry.getKey();
            String dbUrl = entry.getValue();
            DataSource dataSource = createDataSource(dbUrl);
            dataSources.put(tenantId, dataSource);
            LOGGER.info("✅ Initialized DataSource for Tenant: {}", tenantId);
        }

        // ✅ Set the targetDataSources property
        this.setTargetDataSources(new HashMap<>(dataSources));
        this.setDefaultTargetDataSource(
                dataSources.values().iterator().next()); // Set default DB (first in list)
        this.afterPropertiesSet(); // Refresh data sources
    }

    private DataSource createDataSource(String dbUrl) throws Exception {
        HikariDataSource dataSource = new HikariDataSource();
        String decryptedPassword = DecryptionUtil.decrypt(password);
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(decryptedPassword);
        dataSource.setMaximumPoolSize(10);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenant = TenantContextHolder.getCurrentTenant();
        if (tenant == null) {
            log.warn("❌ Tenant not set! Using default target datasource as defined dynamically.");
            // Return null so that the default target datasource (set in initializeDataSources)
            // is used without hardcoding any tenant identifier.
            return null;
        }
        log.info("📌 [MultiTenantDataSource] Resolving tenant before query execution: {}", tenant);
        return tenant;
    }
}
