package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.PrimaryTestDb;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrimaryTestDbRepository extends JpaRepository<PrimaryTestDb, UUID> {}
