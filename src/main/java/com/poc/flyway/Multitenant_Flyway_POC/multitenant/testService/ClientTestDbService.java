package com.poc.flyway.Multitenant_Flyway_POC.multitenant.testService;

import com.adp.benefits.carrier.config.TenantContextHolder;
import com.adp.benefits.carrier.entity.client.ClientTestDb;
import com.adp.benefits.carrier.entity.client.repository.ClientTestDbRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTestDbService {

    private final ClientTestDbRepository clientTestDbRepository;

    @PersistenceContext(unitName = "clientEntityManagerFactory")
    private EntityManager clientEntityManager;

    @Transactional
    public ClientTestDb saveTestDb(ClientTestDb clientTestDb) {
        String tenant = TenantContextHolder.getCurrentTenant();

        if (tenant == null) {
            throw new IllegalStateException("🚨 Tenant not set! Make sure 'X-Tenant-ID' is passed in the request.");
        }

        log.info("📌 Tenant Context before executing query: {}", tenant);

        // Verify if the tenant database is actually being used before saving
        String currentDb = (String) clientEntityManager.createNativeQuery("SELECT current_database()").getSingleResult();
        log.info("📌 Connected to Database BEFORE save: {}", currentDb);

        ClientTestDb savedClientTestDb = clientTestDbRepository.save(clientTestDb);

        // Verify the database AFTER saving to ensure proper switching
        String dbAfterSave = (String) clientEntityManager.createNativeQuery("SELECT current_database()").getSingleResult();
        log.info("📌 Connected to Database AFTER save: {}", dbAfterSave);

        return savedClientTestDb;
    }
}
