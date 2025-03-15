package com.poc.flyway.Multitenant_Flyway_POC.multitenant.testService;

import com.adp.benefits.carrier.entity.client.ClientTestDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientTestDbRepository extends JpaRepository<ClientTestDb, String> {
}
