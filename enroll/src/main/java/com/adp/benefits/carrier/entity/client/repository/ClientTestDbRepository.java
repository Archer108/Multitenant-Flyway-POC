package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.ClientTestDb;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientTestDbRepository extends JpaRepository<ClientTestDb, UUID> {}
