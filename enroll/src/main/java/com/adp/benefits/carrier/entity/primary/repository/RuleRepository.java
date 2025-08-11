package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleRepository extends JpaRepository<Rule, String> {}
