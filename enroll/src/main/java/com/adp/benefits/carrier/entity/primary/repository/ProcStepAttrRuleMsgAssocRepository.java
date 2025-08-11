package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.ProcStepAttrRuleMsgAssoc;
import com.adp.benefits.carrier.entity.primary.ProcStepAttrRuleMsgAssocId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcStepAttrRuleMsgAssocRepository
        extends JpaRepository<ProcStepAttrRuleMsgAssoc, ProcStepAttrRuleMsgAssocId> {}
