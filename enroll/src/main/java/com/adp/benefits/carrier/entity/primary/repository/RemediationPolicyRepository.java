package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.RemediationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RemediationPolicyRepository extends JpaRepository<RemediationPolicy, String> {}
