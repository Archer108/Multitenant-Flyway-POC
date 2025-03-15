package com.poc.flyway.Multitenant_Flyway_POC.multitenant.testService;

import com.adp.benefits.carrier.entity.primary.PrimaryTestDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrimaryTestDbRepository extends JpaRepository<PrimaryTestDb, String> {
}
