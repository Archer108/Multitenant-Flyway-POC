package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.testtmp;

import com.adp.benefits.carrier.config.TenantContextHolder;
import com.adp.benefits.carrier.entity.client.ClientTestDb;
import com.adp.benefits.carrier.entity.client.repository.ClientTestDbRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientTestDbService {

    private final ClientTestDbRepository clientTestDbRepository;

    @PersistenceContext(unitName = "clientEntityManagerFactory")
    private EntityManager clientEntityManager;

    @Transactional
    public ClientTestDb saveTestDb(ClientTestDb clientTestDb) {

        String tenant = TenantContextHolder.getCurrentTenant();

        if (tenant == null) {
            throw new IllegalStateException("Tenant not set! Make sure correct 'X-OID' is passed in the request headers");
        }

        log.info("Tenant Context before executing query: {}", tenant);

        String currentDb = clientEntityManager.createNativeQuery("SELECT current_database()")
                .getSingleResult().toString();
        log.info("Current DB before executing query: {}", currentDb);

        ClientTestDb savedClientTestDb = clientTestDbRepository.save(clientTestDb);

        String dbAfterSave = clientEntityManager.createNativeQuery("SELECT current_database()")
                .getSingleResult().toString();
        log.info("Connected to Database AFTER save: {}", dbAfterSave);

        return savedClientTestDb;
    }
}
