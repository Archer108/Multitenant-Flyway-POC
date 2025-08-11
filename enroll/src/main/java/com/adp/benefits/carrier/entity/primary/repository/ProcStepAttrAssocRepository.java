package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.ProcStepAttrAssoc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcStepAttrAssocRepository extends JpaRepository<ProcStepAttrAssoc, String> {
    @Query(
            "SELECT p FROM ProcStepAttrAssoc p WHERE p.processStep.processStepId = :processStepId AND p.enabledIn = :enabledIn")
    List<ProcStepAttrAssoc> findByProcessStepIdAndEnabledIn(
            @Param("processStepId") String processStepId, @Param("enabledIn") boolean enabledIn);

    //    List<ProcStepAttrAssoc> findByProcessStep_ProcessStepIdAndEnabledIn_(String processStepId,
    // boolean enabledIn);
}
