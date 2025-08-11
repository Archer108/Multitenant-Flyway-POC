package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.RemediationSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RemediationSupportRepository extends JpaRepository<RemediationSupport, String> {}
