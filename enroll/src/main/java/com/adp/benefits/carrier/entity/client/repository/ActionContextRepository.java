package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.ActionContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionContextRepository extends JpaRepository<ActionContext, String> {}
